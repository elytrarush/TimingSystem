package me.makkuusen.timing.system.boatutils;

import org.jetbrains.annotations.Nullable;

public record NonDefaultSetting(String name, Object currentValue, @Nullable Object defaultValue) {}
