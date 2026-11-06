package com.senju.core.cliente;

/** Posiciones del panel en pantalla y cálculo de su origen (sin dependencias de Minecraft). */
public enum Posicion {
    ARRIBA_IZQ, MEDIO_IZQ, ABAJO_IZQ, ARRIBA_DER, MEDIO_DER, ABAJO_DER;

    public Posicion siguiente() {
        Posicion[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public int[] origen(int anchoPant, int altoPant, int anchoPanel, int altoPanel,
                        int margen, int despX, int despY) {
        boolean izq = this == ARRIBA_IZQ || this == MEDIO_IZQ || this == ABAJO_IZQ;
        int x = izq ? margen + despX : anchoPant - anchoPanel - margen - despX;
        int y;
        switch (this) {
            case ARRIBA_IZQ, ARRIBA_DER -> y = margen + despY;
            case MEDIO_IZQ, MEDIO_DER -> y = (altoPant - altoPanel) / 2 + despY;
            default -> {
                y = altoPant - altoPanel - margen - despY;
                // evitar barra rápida, corazones y hambre
                if (x < anchoPant / 2 + 91 && x + anchoPanel > anchoPant / 2 - 91) {
                    y = altoPant - altoPanel - 60 - despY;
                }
            }
        }
        return new int[] {acotar(x, anchoPant - anchoPanel), acotar(y, altoPant - altoPanel)};
    }

    private static int acotar(int v, int max) {
        return Math.max(0, Math.min(v, Math.max(0, max)));
    }
}
