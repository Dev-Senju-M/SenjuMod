package com.senju.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HumoTest {
    @Test
    void modidCorrecto() {
        assertEquals("senjucore", SenjuCore.MODID);
    }
}
