package com.crm.korean_pathway_crm.permission;

import com.crm.korean_pathway_crm.role.entity.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermission {

    @EmbeddedId //khai bao khóa chính gồm roleId và permissionId
    RolePermissionId rolePermissionId ;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    Permission permission;

    @Column(name = "created_at", insertable = false, updatable = false)
    LocalDateTime createdAt;

}
