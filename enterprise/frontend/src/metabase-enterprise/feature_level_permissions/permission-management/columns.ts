import { PermissionSubject } from "metabase/admin/permissions/types";
import { t } from "ttag";

const DOWNLOAD_RESULTS = {
  name: t`Download results`,
  hint: t`If you grant someone permissions to download data from a database, you won't be able to schema or table level for native queries.`,
};

const MANAGE_DATA_MODEL = {
  name: t`Manage data model`,
};

const MANAGE_DATABASES = {
  name: t`Manage databases`,
};

const getDataColumns = (subject: PermissionSubject) => {
  const columns = [DOWNLOAD_RESULTS, MANAGE_DATA_MODEL];

  if (subject === "schemas") {
    columns.push(MANAGE_DATABASES);
  }

  return columns;
};
