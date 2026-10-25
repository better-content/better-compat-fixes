package com.bettercontent.bettercompatfixes.compat.vibration;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

class VibrationInterfaceMixinPluginTest {
    @Test void exactRawAndNamedBindingsOnly() {
        assertDoesNotThrow(()->VibrationInterfaceMixinPlugin.binding("2.4.11", VibrationInterfaceMixinPlugin.CONFIG_SHA,
            VibrationInterfaceMixinPlugin.RAW_CLASS_SHA,true));
        assertDoesNotThrow(()->VibrationInterfaceMixinPlugin.binding("2.4.11", VibrationInterfaceMixinPlugin.CONFIG_SHA,
            VibrationInterfaceMixinPlugin.NAMED_CLASS_SHA,false));
        for(boolean prod:new boolean[]{false,true}) {
            String expected=prod?VibrationInterfaceMixinPlugin.RAW_CLASS_SHA:VibrationInterfaceMixinPlugin.NAMED_CLASS_SHA;
            assertThrows(IllegalStateException.class,()->VibrationInterfaceMixinPlugin.binding("2.4.12",VibrationInterfaceMixinPlugin.CONFIG_SHA,expected,prod));
            assertThrows(IllegalStateException.class,()->VibrationInterfaceMixinPlugin.binding("2.4.11","5d625e4bbcd32da1791dee967786226a217eec8b9f18e2c850547c70067d8fa3",expected,prod));
            assertThrows(IllegalStateException.class,()->VibrationInterfaceMixinPlugin.binding("2.4.11",VibrationInterfaceMixinPlugin.CONFIG_SHA,"e717bf76a3d7931516fc46ecb4a3e32de13fdf126d35f6ffa83d6ab3807c4e37",prod));
            assertThrows(IllegalStateException.class,()->VibrationInterfaceMixinPlugin.binding("2.4.11",VibrationInterfaceMixinPlugin.CONFIG_SHA,prod?VibrationInterfaceMixinPlugin.NAMED_CLASS_SHA:VibrationInterfaceMixinPlugin.RAW_CLASS_SHA,prod));
        }
    }
    @Test void namespaceUnknownTargetsAndUnboundPostApplyFailClosed() {
        var plugin=new VibrationInterfaceMixinPlugin();
        assertDoesNotThrow(()->plugin.onLoad("com.bettercontent.bettercompatfixes.mixin.vibration"));
        assertThrows(IllegalStateException.class,()->plugin.onLoad("other.namespace"));
        assertThrows(IllegalStateException.class,()->plugin.shouldApplyMixin("unknown","unknown"));
        assertThrows(IllegalStateException.class,()->plugin.postApply(VibrationInterfaceTransform.TARGET.replace('/','.'), new ClassNode(),
            "com.bettercontent.bettercompatfixes.mixin.vibration.VibrationTickerMixin",null));
    }
}
