package com.senju.core.staff.castigos;

import com.senju.core.staff.Staff;
import com.senju.core.staff.castigos.RegistroCastigos.Datos;
import com.senju.core.staff.castigos.RegistroCastigos.Posicion;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.BucketItem;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Efectos de CONGELAR y CARCEL. Todo en el hilo del servidor.
 *
 * <ul>
 * <li>Congelar: posición fija ({@code Datos.posCongelado}); cada tick, si se aleja &gt; 0.1 o cambia de dimensión,
 * vuelve (la cabeza gira libre). Sin romper/colocar/atacar/interactuar/usar ítems/contenedores/monturas/perlas/
 * coro/portales.</li>
 * <li>Cárcel: posición de retorno ({@code Datos.retornoCarcel}); cada tick, fuera de radio 8 o de dimensión vuelve al
 * centro. Sin romper/colocar, sin comandos, sin perlas/coro/portales. Al terminar vuelve al retorno (o al spawn si
 * la dimensión ya no existe).</li>
 * <li>Preso y congelado a la vez: la cárcel manda en la posición (el punto congelado se recaptura dentro de la
 * cárcel); el congelado sigue bloqueando acciones.</li>
 * </ul>
 *
 * Login: {@link EfectosCastigos#alEntrar} llama primero a {@link Castigos#procesarVencidos} y DESPUÉS a
 * {@link #alEntrar}, así un castigo vencido nunca se refuerza. Las restauraciones pendientes (perdón o vencimiento
 * con el jugador desconectado) se hacen en {@link #revisar}, que también corre cada tick (idempotente).
 */
public class CongelarYCarcel {
    /** true mientras teletransportamos nosotros: no cancelar viajes ni reaccionar a cambios de dimensión propios. */
    private static boolean teletransportando = false;

    // ---------------------------------------------------------------- registro de efectos

    public static void registrarEfectos() {
        Castigos.registrarEfecto(TipoCastigo.CONGELAR, new EfectoCastigo() {
            @Override
            public void alAplicar(MinecraftServer server, UUID id, @Nullable ServerPlayer online, Castigos.Aplicado datos) {
                RegistroCastigos reg = RegistroCastigos.de(server);
                Datos d = reg.datos(id);
                // Conectado y vivo: se congela donde está. Desconectado o muerto: se captura en el próximo
                // login / tras reaparecer (revisar).
                boolean vivo = online != null && online.isAlive();
                d.posCongelado = vivo ? posicionDe(online) : null;
                reg.setDirty();
                if (vivo && online.isPassenger()) online.stopRiding();
            }

            @Override
            public void alQuitar(MinecraftServer server, UUID id, @Nullable ServerPlayer online) {
                // No hay nada que restaurar: se borra siempre (idempotente, también desconectado).
                RegistroCastigos reg = RegistroCastigos.de(server);
                Datos d = reg.datosSiExisten(id);
                if (d != null && d.posCongelado != null) {
                    d.posCongelado = null;
                    reg.setDirty();
                }
            }
        });
        Castigos.registrarEfecto(TipoCastigo.CARCEL, new EfectoCastigo() {
            @Override
            public Castigos.Resultado puedeAplicar(MinecraftServer server, UUID id, @Nullable ServerPlayer online,
                    TipoCastigo tipo) {
                Posicion c = RegistroCastigos.de(server).carcel();
                return c == null || nivel(server, c.dimension()) == null
                        ? Castigos.Resultado.SIN_CARCEL : Castigos.Resultado.OK;
            }

            @Override
            public void alAplicar(MinecraftServer server, UUID id, @Nullable ServerPlayer online, Castigos.Aplicado datos) {
                RegistroCastigos reg = RegistroCastigos.de(server);
                Datos d = reg.datos(id);
                // (a) Si ya tenía retorno (ya preso, o un retorno pendiente) se conserva el original.
                // Muerto o desconectado: el retorno y el traslado los hace revisar (login / tick tras reaparecer).
                if (online == null || !online.isAlive()) return;
                if (d.retornoCarcel == null) {
                    d.retornoCarcel = posicionDe(online);
                    reg.setDirty();
                }
                if (!Staff.esStaff(online)) llevarACarcel(online, d, reg);
            }

            @Override
            public void alQuitar(MinecraftServer server, UUID id, @Nullable ServerPlayer online) {
                // Desconectado o muerto: se difiere (revisar en login/tick/reaparición). Conectado: se devuelve ya.
                if (online != null) devolverDeCarcel(online);
            }
        });
    }

    // ---------------------------------------------------------------- utilidades

    static Posicion posicionDe(ServerPlayer p) {
        return new Posicion(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(),
                p.getYRot(), p.getXRot());
    }

    @Nullable
    static ServerLevel nivel(MinecraftServer server, String dimension) {
        ResourceLocation rl = ResourceLocation.tryParse(dimension);
        if (rl == null) return null;
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
    }

    /** Dentro del radio de la cárcel (misma dimensión, horizontal y vertical). */
    static boolean dentroCarcel(String dim, double x, double y, double z, Posicion c) {
        return ReglaPosicion.dentroCarcel(dim, x, y, z, c.dimension(), c.x(), c.y(), c.z());
    }

    private static boolean dentroCarcel(ServerPlayer p, Posicion c) {
        return dentroCarcel(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), c);
    }

    /**
     * Teletransporte propio: desmonta, no deja que nuestros propios handlers lo cancelen y anula el daño de caída.
     * {@code rotLibre}: misma dimensión sin tocar la rotación de la cabeza.
     */
    static void teletransportar(ServerPlayer p, ServerLevel lvl, double x, double y, double z, float yRot, float xRot,
            boolean rotLibre) {
        teletransportando = true;
        try {
            if (p.isPassenger()) p.stopRiding();
            if (rotLibre && lvl == p.serverLevel()) {
                p.connection.teleport(x, y, z, p.getYRot(), p.getXRot(),
                        EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
            } else {
                p.teleportTo(lvl, x, y, z, yRot, xRot);
            }
            p.resetFallDistance();
            if (p.isFallFlying()) p.stopFallFlying();
        } finally {
            teletransportando = false;
        }
    }

    private static void teletransportar(ServerPlayer p, ServerLevel lvl, Posicion pos) {
        teletransportar(p, lvl, pos.x(), pos.y(), pos.z(), pos.yRot(), pos.xRot(), false);
    }

    /**
     * Lleva al preso al centro de la cárcel. Si además está congelado, su punto congelado pasa a ser el centro.
     * Devuelve true si hubo teletransporte (false si la cárcel o su dimensión ya no existen).
     */
    private static boolean llevarACarcel(ServerPlayer p, Datos d, RegistroCastigos reg) {
        Posicion c = reg.carcel();
        if (c == null) return false;
        ServerLevel lvl = nivel(p.server, c.dimension());
        if (lvl == null) return false;
        teletransportar(p, lvl, c);
        if (Castigos.activo(p.server, p.getUUID(), TipoCastigo.CONGELAR)) {
            d.posCongelado = posicionDe(p);
            reg.setDirty();
        }
        return true;
    }

    /**
     * Devuelve al jugador a su posición de retorno (o al spawn del mundo si esa dimensión ya no existe) y borra el
     * retorno. Idempotente: sin retorno no hace nada. Si está muerto se deja para la reaparición.
     */
    static void devolverDeCarcel(ServerPlayer p) {
        RegistroCastigos reg = RegistroCastigos.de(p.server);
        Datos d = reg.datosSiExisten(p.getUUID());
        if (d == null || d.retornoCarcel == null) return;
        if (!p.isAlive()) return;
        Posicion r = d.retornoCarcel;
        d.retornoCarcel = null;
        reg.setDirty();
        ServerLevel lvl = nivel(p.server, r.dimension());
        if (lvl != null) {
            teletransportar(p, lvl, r);
        } else {
            ServerLevel ow = p.server.overworld();
            BlockPos sp = ow.getSharedSpawnPos();
            teletransportar(p, ow, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, p.getYRot(), p.getXRot(), false);
        }
        // Si sigue congelado, queda congelado donde aterriza (no lo arrastramos de vuelta a la cárcel).
        if (Castigos.activo(p.server, p.getUUID(), TipoCastigo.CONGELAR)) {
            d.posCongelado = posicionDe(p);
            reg.setDirty();
        }
    }

    /**
     * Restauraciones pendientes y refuerzo de posición. Se llama en el login (después de procesarVencidos), al
     * cambiar de dimensión y cada tick; nunca dentro de PlayerRespawnEvent (ahí connection.player aún es la entidad
     * vieja y el teletransporte no se aplica del todo). {@code avisar}: mostrar "No puedes salir de la cárcel."
     * si se le devolvió al centro. Devuelve true si el jugador está congelado y vigilado (aviso de la barra).
     */
    static boolean revisar(ServerPlayer p, boolean avisar) {
        MinecraftServer server = p.server;
        RegistroCastigos reg = RegistroCastigos.de(server);
        Datos d = reg.datosSiExisten(p.getUUID());
        if (d == null) return false;
        long ahora = System.currentTimeMillis();
        boolean preso = d.estado.activo(TipoCastigo.CARCEL, ahora);
        boolean congelado = d.estado.activo(TipoCastigo.CONGELAR, ahora);

        // (b) Perdón/vencimiento con el jugador desconectado (o muerto): restaurar ahora.
        if (!preso && d.retornoCarcel != null) devolverDeCarcel(p);
        if (!congelado && d.posCongelado != null) {
            d.posCongelado = null;
            reg.setDirty();
        }
        if (!preso && !congelado) return false;
        if (!p.isAlive() || Staff.esStaff(p)) return false;

        if (preso) {
            Posicion c = reg.carcel();
            if (c != null) {
                if (d.retornoCarcel == null) {
                    // Encarcelado mientras estaba desconectado: el retorno es donde aparece.
                    d.retornoCarcel = posicionDe(p);
                    reg.setDirty();
                }
                if (!dentroCarcel(p, c) && llevarACarcel(p, d, reg) && avisar) {
                    p.displayClientMessage(Component.translatable("senjucore.castigo.carcel.limite"), true);
                }
            }
        }

        if (congelado) {
            Posicion f = d.posCongelado;
            Posicion c = preso ? reg.carcel() : null;
            boolean invalido = f == null || nivel(server, f.dimension()) == null
                    || (c != null && !dentroCarcel(f.dimension(), f.x(), f.y(), f.z(), c));
            if (invalido) {
                // Primer login congelado, dimensión desaparecida o congelado fuera de la cárcel: se fija aquí.
                d.posCongelado = posicionDe(p);
                reg.setDirty();
            } else {
                if (p.isPassenger()) p.stopRiding();
                if (ReglaPosicion.seMovio(p.level().dimension().location().toString(), p.getX(), p.getY(),
                        p.getZ(), f.dimension(), f.x(), f.y(), f.z())) {
                    ServerLevel lvl = nivel(server, f.dimension());
                    teletransportar(p, lvl, f.x(), f.y(), f.z(), f.yRot(), f.xRot(), true);
                }
            }
            if (p.containerMenu != p.inventoryMenu) p.closeContainer();
            return true;
        }
        return false;
    }

    /** Login: llamado por {@link EfectosCastigos#alEntrar} DESPUÉS de procesarVencidos y del control de ban. */
    public static void alEntrar(ServerPlayer p) {
        revisar(p, false);
    }

    /** Recién reaparecidos: su primer tick hace la revisión (y el traslado) sin el aviso de fuga. */
    private static final Set<UUID> REAPARECIDOS = new HashSet<>();

    // ---------------------------------------------------------------- consultas rápidas

    private static boolean congelado(Entity e) {
        return e instanceof ServerPlayer p && Castigos.activo(p.server, p.getUUID(), TipoCastigo.CONGELAR)
                && !Staff.esStaff(p);
    }

    private static boolean preso(Entity e) {
        return e instanceof ServerPlayer p && Castigos.activo(p.server, p.getUUID(), TipoCastigo.CARCEL)
                && !Staff.esStaff(p);
    }

    private static void cancelar(Event ev) {
        if (ev.isCancelable()) ev.setCanceled(true);
    }

    // ---------------------------------------------------------------- eventos

    @SubscribeEvent
    public void alTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !ev.side.isServer()) return;
        if (!(ev.player instanceof ServerPlayer p)) return;
        boolean congelado = revisar(p, !REAPARECIDOS.remove(p.getUUID()));
        if (congelado && p.tickCount % 20 == 0) {
            p.displayClientMessage(Component.translatable("senjucore.castigo.congelado"), true);
        }
    }

    /**
     * Reaparecer: NO se teletransporta aquí (connection.player aún apunta a la entidad vieja); el siguiente tick
     * llama a revisar (preso -> cárcel; retorno pendiente -> retorno). Un congelado que no está preso se recongela
     * donde reaparece (evita el bucle de muertes si su punto era letal).
     */
    @SubscribeEvent
    public void alReaparecer(PlayerEvent.PlayerRespawnEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        REAPARECIDOS.add(p.getUUID());
        RegistroCastigos reg = RegistroCastigos.de(p.server);
        Datos d = reg.datosSiExisten(p.getUUID());
        long ahora = System.currentTimeMillis();
        if (d != null && d.posCongelado != null && d.estado.activo(TipoCastigo.CONGELAR, ahora)
                && !d.estado.activo(TipoCastigo.CARCEL, ahora)) {
            d.posCongelado = null;
            reg.setDirty();
        }
    }

    @SubscribeEvent
    public void alSalir(PlayerEvent.PlayerLoggedOutEvent ev) {
        REAPARECIDOS.remove(ev.getEntity().getUUID());
    }

    @SubscribeEvent
    public void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent ev) {
        if (teletransportando) return;
        if (ev.getEntity() instanceof ServerPlayer p) revisar(p, true);
    }

    /** Ni vaciar ni llenar cubos (BucketItem dispara FillBucketEvent, no EntityPlaceEvent). */
    @SubscribeEvent
    public void alCubo(FillBucketEvent ev) {
        if (congelado(ev.getEntity()) || preso(ev.getEntity())) cancelar(ev);
    }

    /** El congelado no puede defenderse: no recibe ataques (salvo daño que ignora la invulnerabilidad: vacío, /kill). */
    @SubscribeEvent
    public void alSerAtacado(LivingAttackEvent ev) {
        if (ev.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (congelado(ev.getEntity())) cancelar(ev);
    }

    /** Portales (y cualquier viaje que pase por ForgeHooks.onTravelToDimension) salvo nuestros teletransportes. */
    @SubscribeEvent
    public void alViajar(EntityTravelToDimensionEvent ev) {
        if (teletransportando) return;
        if (congelado(ev.getEntity()) || preso(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alPerla(EntityTeleportEvent.EnderPearl ev) {
        if (congelado(ev.getEntity()) || preso(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alCoro(EntityTeleportEvent.ChorusFruit ev) {
        if (congelado(ev.getEntity()) || preso(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alMontar(EntityMountEvent ev) {
        if (ev.isMounting() && congelado(ev.getEntityMounting())) cancelar(ev);
    }

    @SubscribeEvent
    public void alRomper(BlockEvent.BreakEvent ev) {
        if (congelado(ev.getPlayer()) || preso(ev.getPlayer())) cancelar(ev);
    }

    @SubscribeEvent
    public void alColocar(BlockEvent.EntityPlaceEvent ev) {
        if (congelado(ev.getEntity()) || preso(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alAtacar(AttackEntityEvent ev) {
        if (congelado(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alClicDerechoBloque(PlayerInteractEvent.RightClickBlock ev) {
        if (congelado(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alClicDerechoItem(PlayerInteractEvent.RightClickItem ev) {
        if (congelado(ev.getEntity())
                || (ev.getItemStack().getItem() instanceof BucketItem && preso(ev.getEntity()))) {
            cancelar(ev);
        }
    }

    @SubscribeEvent
    public void alClicIzquierdoBloque(PlayerInteractEvent.LeftClickBlock ev) {
        if (congelado(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alInteractuarEntidad(PlayerInteractEvent.EntityInteract ev) {
        if (congelado(ev.getEntity())) cancelar(ev);
    }

    @SubscribeEvent
    public void alInteractuarEntidadEspecifica(PlayerInteractEvent.EntityInteractSpecific ev) {
        if (congelado(ev.getEntity())) cancelar(ev);
    }

    /** Open no se puede cancelar: se cierra en el acto (y el tick lo vuelve a cerrar si hiciera falta). */
    @SubscribeEvent
    public void alAbrirContenedor(PlayerContainerEvent.Open ev) {
        if (ev.getEntity() instanceof ServerPlayer p && congelado(p) && p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
    }

    /** Preso: ningún comando (los del staff no se ven afectados porque el staff nunca está preso). */
    @SubscribeEvent
    public void alComando(CommandEvent ev) {
        if (!(ev.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer p)) return;
        if (!preso(p)) return;
        ev.setCanceled(true);
        p.sendSystemMessage(Component.translatable("senjucore.castigo.carcel.comando"));
    }
}
