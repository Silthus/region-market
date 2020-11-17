package net.silthus.regions.entities;

import io.ebean.annotation.DbEnumValue;

public enum AccessLevel {

    OWNER,
    MEMBER,
    GUEST;

    @DbEnumValue
    public String getValue() {
        return name();
    }
}
