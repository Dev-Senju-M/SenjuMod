package com.senju.core.vidas;

import com.mojang.authlib.GameProfile;
import com.senju.core.config.CommonConfig;
import com.senju.core.network.PaqueteAviso;
import com.senju.core.network.PaqueteVidas;
import com.senju.core.network.Red;
import com.senju.core.staff.castigos.Castigos;
import com.senju.core.staff.castigos.RegistroCastigos.Posicion;
import com.senju.core.vidas.RegistroVidas.Datos;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Servicio de vidas. Todo en el hilo del servidor. */
public final class Vidas {
    private Vidas() {}

    /** Jugadores a revisar en su próximo tick vivo (reaparecidos eliminados, revividos mientras estaban muertos). */
    static final Set<UUID> REVISAR = new HashSet<>();

    // ---------------------------------------------------------------- consulta

    /** Vidas actuales (crea el registro con {@code vidasIniciales} si es la primera vez). */
    public static int vidas(MinecraftServer server, UUID id) {
        return RegistroVidas.de(server).datos(id).vidas;
    }

    /** Total para mostrar en el HUD: las iniciales, o más si tiene más vidas. */
    public static int total(int vidas) {
        return Math.max(CommonConfig.VIDAS_INICIALES.get(), vidas);
    }

    /** false si es staff y {@code vidasStaff=false} (exento: no pierde vidas ni puede quedar eliminado). */
    public static boolean cuenta(MinecraftServer server, GameProfile perfil) {
        return CommonConfig.VIDAS_STAFF.get() || !Castigos.esStaff(server, perfil);
    }

    public static boolean cuenta(ServerPlayer p) {
        return cuenta(p.server, p.getGameProfile());
    }

    /** Eliminado de verdad (0 vidas y no exento). */
    public static boolean eliminado(MinecraftServer server, GameProfile perfil) {
        return perfil.getId() != null && RegistroVidas.de(server).eliminado(perfil.getId()) && cuenta(server, perfil);
    }

    // ---------------------------------------------------------------- avisos y paquetes

    public static void avisarATodos(MinecraftServer server, PaqueteAviso a) {
        Red.CANAL.send(PacketDistributor.ALL.noArg(), a);
    }

    /** Envía sus vidas al HUD del propio jugador (-1 si está exento). */
    public static void enviarVidas(ServerPlayer p) {
        if (!cuenta(p)) {
            Red.enviarA(p, new PaqueteVidas(-1, 0));
            return;
        }
        int v = vidas(p.server, p.getUUID());
        Red.enviarA(p, new PaqueteVidas(v, total(v)));
    }

    private static String nombreDe(GameProfile gp, Datos d) {
        if (gp.getName() != null && !gp.getName().isEmpty()) return gp.getName();
        return d.nombre.isEmpty() ? String.valueOf(gp.getId()) : d.nombre;
    }

    // ---------------------------------------------------------------- muerte

    /**
     * Cuenta una muerte (llamar una sola vez por muerte real). No hace nada si el jugador está exento o ya
     * eliminado. Al llegar a 0 queda eliminado: espectador en cuanto reaparezca.
     */
    public static void alMorir(ServerPlayer p) {
        MinecraftServer server = p.server;
        if (!cuenta(p)) return;
        RegistroVidas reg = RegistroVidas.de(server);
        Datos d = reg.datos(p.getUUID());
        d.nombre = p.getGameProfile().getName();
        if (ReglaVidas.eliminado(d.vidas)) return;
        d.vidas = ReglaVidas.restar(d.vidas);
        reg.setDirty();
        if (ReglaVidas.eliminado(d.vidas)) REVISAR.add(p.getUUID());
        avisarATodos(server, new PaqueteAviso(d.nombre, d.vidas, total(d.vidas), ReglaVidas.avisoTrasMuerte(d.vidas)));
        enviarVidas(p);
    }

    // ---------------------------------------------------------------- fijar / revivir

    /** Fija las vidas. 0 elimina; mayor que 0 desde 0 revive (sin lugar); si no, solo cambia el número. */
    public static void fijar(MinecraftServer server, GameProfile gp, int n) {
        UUID id = gp.getId();
        if (id == null) return;
        n = Math.max(0, Math.min(100, n));
        RegistroVidas reg = RegistroVidas.de(server);
        Datos d = reg.datos(id);
        d.nombre = nombreDe(gp, d);
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        boolean eraEliminado = ReglaVidas.eliminado(d.vidas);
        if (eraEliminado && n > 0) {
            revivir(server, gp, n, null);
            return;
        }
        d.vidas = n;
        if (n == 0) {
            // Un 0 manual cancela cualquier revivir pendiente.
            d.pendienteRevivir = 0;
            d.posRevivir = null;
        }
        reg.setDirty();
        if (online != null) {
            if (n == 0 && !eraEliminado && cuenta(online)) {
                REVISAR.add(id);
                avisarATodos(server, new PaqueteAviso(d.nombre, 0, total(0), TipoAviso.ELIMINADO));
            }
            enviarVidas(online);
        }
    }

    /**
     * Revive con {@code vidas} vidas, en {@code lugar} (o, sin lugar, en su punto de reaparición). Conectado y vivo:
     * en el acto. Desconectado o muerto: queda pendiente y se aplica al entrar / en su primer tick vivo.
     */
    public static void revivir(MinecraftServer server, GameProfile gp, int vidas, @Nullable Posicion lugar) {
        UUID id = gp.getId();
        if (id == null) return;
        RegistroVidas reg = RegistroVidas.de(server);
        Datos d = reg.datos(id);
        d.nombre = nombreDe(gp, d);
        d.pendienteRevivir = Math.max(1, Math.min(100, vidas));
        d.posRevivir = lugar;
        reg.setDirty();
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online == null) return;
        if (online.isAlive()) aplicarPendiente(online);
        else REVISAR.add(id);
    }

    /**
     * Aplica el revivir pendiente a un jugador conectado y vivo: vidas, supervivencia, traslado y aviso.
     * Devuelve false si no había nada pendiente.
     */
    static boolean aplicarPendiente(ServerPlayer p) {
        MinecraftServer server = p.server;
        RegistroVidas reg = RegistroVidas.de(server);
        Datos d = reg.datosSiExisten(p.getUUID());
        if (d == null || d.pendienteRevivir <= 0) return false;
        int n = d.pendienteRevivir;
        Posicion lugar = d.posRevivir;
        d.vidas = n;
        d.pendienteRevivir = 0;
        d.posRevivir = null;
        d.nombre = p.getGameProfile().getName();
        reg.setDirty();
        REVISAR.remove(p.getUUID());
        p.setGameMode(GameType.SURVIVAL);
        if (!teletransportar(p, lugar)) teletransportarAReaparicion(p);
        p.resetFallDistance();
        enviarVidas(p);
        avisarATodos(server, new PaqueteAviso(d.nombre, n, total(n), TipoAviso.RESUCITADO));
        return true;
    }

    private static boolean teletransportar(ServerPlayer p, @Nullable Posicion pos) {
        if (pos == null) return false;
        ResourceLocation rl = ResourceLocation.tryParse(pos.dimension());
        ServerLevel lvl = rl == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        if (lvl == null) return false;
        if (p.isPassenger()) p.stopRiding();
        p.teleportTo(lvl, pos.x(), pos.y(), pos.z(), pos.yRot(), pos.xRot());
        return true;
    }

    /** Punto de reaparición del jugador (cama/ancla válidas) o el spawn del mundo. */
    private static void teletransportarAReaparicion(ServerPlayer p) {
        MinecraftServer server = p.server;
        BlockPos rp = p.getRespawnPosition();
        if (rp != null) {
            ServerLevel lvl = server.getLevel(p.getRespawnDimension());
            if (lvl != null) {
                Optional<Vec3> v = Player.findRespawnPositionAndUseSpawnBlock(lvl, rp, p.getRespawnAngle(),
                        p.isRespawnForced(), true);
                if (v.isPresent()) {
                    if (p.isPassenger()) p.stopRiding();
                    p.teleportTo(lvl, v.get().x, v.get().y, v.get().z, p.getRespawnAngle(), 0.0F);
                    return;
                }
            }
        }
        ServerLevel ow = server.overworld();
        BlockPos sp = ow.getSharedSpawnPos();
        if (p.isPassenger()) p.stopRiding();
        p.teleportTo(ow, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, p.getYRot(), 0.0F);
    }
}
