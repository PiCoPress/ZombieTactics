package net.picopress.mc.mods.zombietactics2.fabric;

import net.picopress.mc.mods.zombietactics2.impl.IMain;
import net.picopress.mc.mods.zombietactics2.commands.CommandSumZ;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import eu.midnightdust.lib.config.MidnightConfig;


// initialize the mod for fabric
public class Main implements ModInitializer, IMain {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, access, env) -> CommandSumZ.register(dispatcher)
        );
        MidnightConfig.init(MOD_ID, FabricConfig.class);
        FabricConfig.updateConfig();
    }
}
