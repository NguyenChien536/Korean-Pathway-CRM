package com.crm.korean_pathway_crm.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermissionId implements Serializable {
    //role_id         uuid NOT NULL REFERENCES roles(id),
    //    permission_id   uuid NOT NULL REFERENCES permissions(id),
    //    created_at      timestamptz NOT NULL DEFAULT now(),
    //    PRIMARY KEY (role_id, permission_id)

    @Column(name = "role_id")
    UUID roleId;
    @Column(name = "permission_id")
    UUID permissionId;
}
