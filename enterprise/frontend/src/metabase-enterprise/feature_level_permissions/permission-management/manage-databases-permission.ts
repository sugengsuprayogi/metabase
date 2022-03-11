import { GroupsPermissions } from "metabase-types/api";
import { UNABLE_TO_CHANGE_ADMIN_PERMISSIONS } from "metabase/admin/permissions/constants/messages";
import { EntityId, PermissionSubject } from "metabase/admin/permissions/types";
import {
  getSchemasPermission,
  isRestrictivePermission,
} from "metabase/admin/permissions/utils/graph";
import { t } from "ttag";

export const MANAGE_DATABASES_PERMISSION_REQUIRES_DATA_ACCESS = t`Database management access requires full data access.`;

export const MANAGE_DATABASES_PERMISSION_OPTIONS = {
  none: {
    label: t`No`,
    value: "none",
    icon: "close",
    iconColor: "danger",
  },
  view: {
    label: t`View`,
    value: "view",
    icon: "eye",
    iconColor: "accent7",
  },
  edit: {
    label: t`Edit`,
    value: "edit",
    icon: "pencil",
    iconColor: "accent7",
  },
};

export const buildManageDatabasesPermission = (
  entityId: EntityId,
  groupId: number,
  isAdmin: boolean,
  permissions: GroupsPermissions,
  dataAccessPermissionValue: string,
  permissionSubject: PermissionSubject,
) => {
  if (permissionSubject !== "schemas") {
    return null;
  }

  const value = isRestrictivePermission(dataAccessPermissionValue)
    ? MANAGE_DATABASES_PERMISSION_OPTIONS.none.value
    : getSchemasPermission(permissions, groupId, entityId, "manage-databases");

  const isDisabled =
    isAdmin || isRestrictivePermission(dataAccessPermissionValue);

  return {
    permission: "manage-databases",
    type: "manage-databases",
    isDisabled,
    disabledTooltip: isAdmin
      ? UNABLE_TO_CHANGE_ADMIN_PERMISSIONS
      : MANAGE_DATABASES_PERMISSION_REQUIRES_DATA_ACCESS,
    isHighlighted: isAdmin,
    value,
    options: [
      MANAGE_DATABASES_PERMISSION_OPTIONS.none,
      MANAGE_DATABASES_PERMISSION_OPTIONS.view,
      MANAGE_DATABASES_PERMISSION_OPTIONS.edit,
    ],
  };
};
