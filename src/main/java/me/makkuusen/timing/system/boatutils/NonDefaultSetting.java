package me.makkuusen.timing.system.boatutils;

import javax.annotation.Nullable;

public record NonDefaultSetting(String name, Object currentValue, @Nullable Object defaultValue) {}
