export interface EditorPayload {
  version: number;
  metadata: EditorMetadata;
  jobs: Record<string, JobData>;
  registeredActionTypes: string[];
  registeredPayableTypes: string[];
}

export interface EditorMetadata {
  exportedAt: string;
  exportedBy: string;
  sessionToken: string;
  serverName: string | null;
}

export interface JobData {
  displayName: string;
  tasks: TaskData[];
}

export interface TaskData {
  actionTypeKey: string;
  contextKey: string;
  payables: PayableData[];
}

export interface PayableData {
  type: string;
  amount: string;
}
