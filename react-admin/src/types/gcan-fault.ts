import type { ApiPageRequest, ApiStatus } from "./api";

export type GcanFaultProfileRecord = {
  id: number;
  profileCode: string;
  profileName: string;
  manufacturer?: string | null;
  vehicleType?: string | null;
  protocolVersion?: string | null;
  applicableVehicleDescription?: string | null;
  status: ApiStatus;
  remark?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
};

export type GcanFaultDefinitionRecord = {
  id: number;
  profileCode: string;
  faultCode: string;
  rawLevelCode?: string | null;
  rawLevelName?: string | null;
  faultName?: string | null;
  faultDefinition?: string | null;
  analysis?: string | null;
  symptom?: string | null;
  recovery?: string | null;
  removal?: string | null;
  handlingSuggestion?: string | null;
  remark?: string | null;
  status: ApiStatus;
  createTime?: string | null;
  updateTime?: string | null;
};

export type GcanFaultProfileQuery = Partial<ApiPageRequest> & {
  profileCode?: string;
  profileName?: string;
  manufacturer?: string;
  vehicleType?: string;
  status?: ApiStatus;
};

export type GcanFaultDefinitionQuery = Partial<ApiPageRequest> & {
  profileCode?: string;
  faultCode?: string;
  faultName?: string;
  status?: ApiStatus;
};

export type GcanFaultProfileSaveRequest = {
  profileCode: string;
  profileName: string;
  manufacturer?: string;
  vehicleType?: string;
  protocolVersion?: string;
  applicableVehicleDescription?: string;
  status?: ApiStatus;
  remark?: string;
};

export type GcanFaultDefinitionSaveRequest = {
  profileCode: string;
  faultCode: string;
  rawLevelCode?: string;
  rawLevelName?: string;
  faultName?: string;
  faultDefinition?: string;
  analysis?: string;
  symptom?: string;
  recovery?: string;
  removal?: string;
  handlingSuggestion?: string;
  remark?: string;
  status?: ApiStatus;
};
