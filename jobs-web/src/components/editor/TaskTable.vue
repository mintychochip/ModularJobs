<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import type { EditorPayload, TaskData } from '../../lib/types';
import { fetchSession, saveSession } from '../../lib/bytebin';

const loading = ref(true);
const error = ref<string | null>(null);
const payload = ref<EditorPayload | null>(null);
const saving = ref(false);
const savedCode = ref<string | null>(null);
const sessionCode = ref<string | null>(null);

// Flatten tasks for table display
const flatTasks = computed(() => {
  if (!payload.value) return [];
  const tasks: Array<{ jobKey: string; jobName: string; task: TaskData; taskIndex: number }> = [];
  for (const [jobKey, jobData] of Object.entries(payload.value.jobs)) {
    jobData.tasks.forEach((task, index) => {
      tasks.push({ jobKey, jobName: jobData.displayName, task, taskIndex: index });
    });
  }
  return tasks;
});

onMounted(async () => {
  // Get code from URL query parameter
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');

  if (!code) {
    error.value = 'No session code provided. Add ?code=YOUR_CODE to the URL.';
    loading.value = false;
    return;
  }

  sessionCode.value = code;

  try {
    payload.value = await fetchSession(code);
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load session';
  } finally {
    loading.value = false;
  }
});

function updateContext(jobKey: string, taskIndex: number, newContext: string) {
  if (!payload.value) return;
  payload.value.jobs[jobKey].tasks[taskIndex].contextKey = newContext;
}

function updatePayableAmount(jobKey: string, taskIndex: number, payableIndex: number, newAmount: string) {
  if (!payload.value) return;
  payload.value.jobs[jobKey].tasks[taskIndex].payables[payableIndex].amount = newAmount;
}

function deleteTask(jobKey: string, taskIndex: number) {
  if (!payload.value) return;
  payload.value.jobs[jobKey].tasks.splice(taskIndex, 1);
}

function addTask(jobKey: string) {
  if (!payload.value) return;
  const newTask: TaskData = {
    actionTypeKey: payload.value.registeredActionTypes[0] || 'modularjobs:block_break',
    contextKey: 'minecraft:stone',
    payables: [{ type: payload.value.registeredPayableTypes[0] || 'modularjobs:experience', amount: '1.0' }]
  };
  payload.value.jobs[jobKey].tasks.push(newTask);
}

async function save() {
  if (!payload.value) return;
  saving.value = true;
  try {
    const newCode = await saveSession(payload.value);
    savedCode.value = newCode;
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save';
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div>
    <!-- Loading state -->
    <div v-if="loading" class="flex justify-center p-8">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="alert alert-error">
      <span>{{ error }}</span>
    </div>

    <!-- Editor -->
    <div v-else-if="payload">
      <!-- Success message after save -->
      <div v-if="savedCode" class="alert alert-success mb-4">
        <div>
          <span>Saved! Run in-game: </span>
          <code class="font-mono">/jobs applyedits {{ savedCode }}</code>
        </div>
      </div>

      <!-- Header with save button -->
      <div class="flex justify-between items-center mb-4">
        <h1 class="text-2xl font-bold">Job Tasks Editor</h1>
        <button @click="save" :disabled="saving" class="btn btn-primary">
          <span v-if="saving" class="loading loading-spinner"></span>
          {{ saving ? 'Saving...' : 'Save Changes' }}
        </button>
      </div>

      <!-- Tasks table -->
      <div class="overflow-x-auto">
        <table class="table table-zebra w-full">
          <thead>
            <tr>
              <th>Job</th>
              <th>Action Type</th>
              <th>Context</th>
              <th>Payables</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in flatTasks" :key="`${item.jobKey}-${item.taskIndex}`">
              <td>{{ item.jobName }}</td>
              <td>
                <select v-model="item.task.actionTypeKey" class="select select-bordered select-sm w-full">
                  <option v-for="at in payload.registeredActionTypes" :key="at" :value="at">
                    {{ at.split(':')[1] }}
                  </option>
                </select>
              </td>
              <td>
                <input type="text" v-model="item.task.contextKey"
                       class="input input-bordered input-sm w-full" />
              </td>
              <td>
                <div v-for="(payable, pIndex) in item.task.payables" :key="pIndex" class="flex gap-1 mb-1">
                  <span class="badge badge-outline">{{ payable.type.split(':')[1] }}</span>
                  <input type="text" v-model="payable.amount"
                         class="input input-bordered input-xs w-20" />
                </div>
              </td>
              <td>
                <button @click="deleteTask(item.jobKey, item.taskIndex)" class="btn btn-error btn-xs">
                  Delete
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Add task buttons per job -->
      <div class="mt-4 space-y-2">
        <div v-for="(jobData, jobKey) in payload.jobs" :key="jobKey" class="flex items-center gap-2">
          <span class="font-medium">{{ jobData.displayName }}:</span>
          <button @click="addTask(jobKey as string)" class="btn btn-success btn-sm">+ Add Task</button>
        </div>
      </div>
    </div>
  </div>
</template>
