package com.crm.korean_pathway_crm.permission;

import org.mapstruct.*;

@Mapper(componentModel = "spring" )
public interface PermissionMapper {
    //Anh xa request sang entity
    //ignore cac field khong nen nhan tu request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updatePermission(@MappingTarget Permission permission, PermissionRequest request);

}
