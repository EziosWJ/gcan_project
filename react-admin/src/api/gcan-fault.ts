import type { ApiPageResult } from "@/types/api";
import { http } from "@/lib/http";
import type {
  GcanFaultDefinitionQuery,
  GcanFaultDefinitionRecord,
  GcanFaultDefinitionSaveRequest,
  GcanFaultProfileQuery,
  GcanFaultProfileRecord,
  GcanFaultProfileSaveRequest,
} from "@/types/gcan-fault";

const PROFILE_PATH = "/api/gcan/fault-profile";
const DEFINITION_PATH = "/api/gcan/fault-definition";

export function getGcanFaultProfilePage(query: GcanFaultProfileQuery) {
  return http.get<ApiPageResult<GcanFaultProfileRecord>>(`${PROFILE_PATH}/page`, { query });
}

export function getGcanFaultProfilesEnabled() {
  return http.get<GcanFaultProfileRecord[]>(`${PROFILE_PATH}/enabled`);
}

export function createGcanFaultProfile(data: GcanFaultProfileSaveRequest) {
  return http.post<void>(PROFILE_PATH, data);
}

export function updateGcanFaultProfile(id: number, data: GcanFaultProfileSaveRequest) {
  return http.put<void>(`${PROFILE_PATH}/${id}`, data);
}

export function deleteGcanFaultProfile(id: number) {
  return http.delete<void>(`${PROFILE_PATH}/${id}`);
}

export function updateGcanFaultProfileStatus(id: number, status: number) {
  return http.patch<void>(`${PROFILE_PATH}/${id}/status`, { status });
}

export function getGcanFaultDefinitionPage(query: GcanFaultDefinitionQuery) {
  return http.get<ApiPageResult<GcanFaultDefinitionRecord>>(`${DEFINITION_PATH}/page`, { query });
}

export function createGcanFaultDefinition(data: GcanFaultDefinitionSaveRequest) {
  return http.post<void>(DEFINITION_PATH, data);
}

export function updateGcanFaultDefinition(id: number, data: GcanFaultDefinitionSaveRequest) {
  return http.put<void>(`${DEFINITION_PATH}/${id}`, data);
}

export function deleteGcanFaultDefinition(id: number) {
  return http.delete<void>(`${DEFINITION_PATH}/${id}`);
}

export function updateGcanFaultDefinitionStatus(id: number, status: number) {
  return http.patch<void>(`${DEFINITION_PATH}/${id}/status`, { status });
}
