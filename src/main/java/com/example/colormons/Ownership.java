package com.example.colormons;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Whether the player may colour a Pokemon.
 * Unknowable cases are allowed - blocking the player's own Pokemon is the worse error.
 */
public class Ownership {
    public static final int MINE = 0;
    public static final int WILD = 1;
    public static final int OTHER = 2;

    public static int check(Minecraft mc, PokemonEntity entity) {
        if (mc.player == null || entity == null) return OTHER;
        UUID me = mc.player.getUUID();

        if (inMyParty(mc, entity)) return MINE;

        UUID owner = ownerOf(entity.getPokemon());
        if (owner != null) return owner.equals(me) ? MINE : OTHER;

        try {
            Player p = entity.getOwner();
            if (p != null) return p.getUUID().equals(me) ? MINE : OTHER;
        } catch (Throwable ignored) {}

        // nothing says otherwise: treat as wild
        return WILD;
    }

    private static boolean inMyParty(Minecraft mc, PokemonEntity entity) {
        try {
            Object party = mc.player.getClass().getMethod("getParty").invoke(mc.player);
            if (party instanceof Iterable<?> it) {
                UUID target = entity.getPokemon().getUuid();
                for (Object mon : it) {
                    Object u = call(mon, "getUuid");
                    if (u instanceof UUID id && id.equals(target)) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static UUID ownerOf(Pokemon mon) {
        try {
            UUID u = mon.getOwnerUUID();
            if (u != null) return u;
        } catch (Throwable ignored) {}

        for (String name : new String[]{"getOwnerUUID", "getOriginalTrainerUUID", "getOwnerId"}) {
            Object v = call(mon, name);
            if (v instanceof UUID u) return u;
            if (v instanceof String s) {
                try { return UUID.fromString(s); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static Object call(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }
}
