package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionBoatUtilsMode implements Permissions {
    CREATE,
    EDIT,
    SAVE;

    @Override
    public String getNode() {
        return "timingsystem.boatutilsmode." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for(PermissionBoatUtilsMode perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
