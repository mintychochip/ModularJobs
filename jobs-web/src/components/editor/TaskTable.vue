<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch } from 'vue';
import type { EditorPayload, TaskData, PayableData } from '../../lib/types';
import { fetchSession, saveSession } from '../../lib/bytebin';

const loading = ref(true);
const error = ref<string | null>(null);
const payload = ref<EditorPayload | null>(null);
const saving = ref(false);
const savedCode = ref<string | null>(null);
const sessionCode = ref<string | null>(null);

// Tab navigation
const selectedJobKey = ref<string | null>(null);

// Keyboard navigation state
const focusedCell = ref<{ rowIndex: number; field: string } | null>(null);
const cellRefs = ref<Map<string, HTMLInputElement | HTMLSelectElement>>(new Map());

// Payable editor popover state
const payablePopoverTarget = ref<{ jobKey: string; taskIndex: number } | null>(null);

// Get sorted job keys for tabs
const jobKeys = computed(() => {
  if (!payload.value) return [];
  return Object.keys(payload.value.jobs).sort((a, b) =>
    payload.value!.jobs[a].displayName.localeCompare(payload.value!.jobs[b].displayName)
  );
});

// Get currently selected job data
const selectedJobData = computed(() => {
  if (!payload.value || !selectedJobKey.value) return null;
  return payload.value.jobs[selectedJobKey.value];
});

// Set first job as default when payload loads
watch(payload, (newPayload) => {
  if (newPayload && !selectedJobKey.value && jobKeys.value.length > 0) {
    selectedJobKey.value = jobKeys.value[0];
  }
}, { immediate: true });

// Format payable for display
function formatPayable(payable: PayableData): string {
  const type = payable.type.split(':')[1] || payable.type;
  return `${type}: ${payable.amount}`;
}

// Get all payables summary
function getPayablesSummary(payables: PayableData[]): string {
  if (payables.length === 0) return 'No payables';
  if (payables.length === 1) return formatPayable(payables[0]);
  return `${payables.length} payables: ${payables.map(p => p.amount).join(', ')}`;
}

// Task operations
function deleteTask(jobKey: string, taskIndex: number) {
  if (!payload.value) return;
  payload.value.jobs[jobKey].tasks.splice(taskIndex, 1);
}

function addTask(jobKey: string) {
  if (!payload.value) return;
  const newTask: TaskData = {
    actionTypeKey: payload.value.registeredActionTypes[0] || 'modularjobs:block_break',
    contextKey: '',
    payables: [{ type: payload.value.registeredPayableTypes[0] || 'modularjobs:experience', amount: '1.0' }]
  };
  payload.value.jobs[jobKey].tasks.push(newTask);

  // Focus the new row
  nextTick(() => {
    const newRowIndex = payload.value!.jobs[jobKey].tasks.length - 1;
    focusCell(newRowIndex, 'actionType');
  });
}

function duplicateTask(jobKey: string, taskIndex: number) {
  if (!payload.value) return;
  const original = payload.value.jobs[jobKey].tasks[taskIndex];
  const duplicate: TaskData = JSON.parse(JSON.stringify(original));
  payload.value.jobs[jobKey].tasks.splice(taskIndex + 1, 0, duplicate);
}

// Payable operations
function addPayable(jobKey: string, taskIndex: number) {
  if (!payload.value) return;
  const newPayable: PayableData = {
    type: payload.value.registeredPayableTypes[0] || 'modularjobs:experience',
    amount: '1.0'
  };
  payload.value.jobs[jobKey].tasks[taskIndex].payables.push(newPayable);
}

function deletePayable(jobKey: string, taskIndex: number, payableIndex: number) {
  if (!payload.value) return;
  const payables = payload.value.jobs[jobKey].tasks[taskIndex].payables;
  if (payables.length > 1) {
    payables.splice(payableIndex, 1);
  }
}

function updatePayableType(jobKey: string, taskIndex: number, payableIndex: number, newType: string) {
  if (!payload.value) return;
  payload.value.jobs[jobKey].tasks[taskIndex].payables[payableIndex].type = newType;
}

function updatePayableAmount(jobKey: string, taskIndex: number, payableIndex: number, newAmount: string) {
  if (!payload.value) return;
  payload.value.jobs[jobKey].tasks[taskIndex].payables[payableIndex].amount = newAmount;
}

// Keyboard navigation
function focusCell(rowIndex: number, field: string) {
  focusedCell.value = { rowIndex, field };
  nextTick(() => {
    const key = `${rowIndex}-${field}`;
    const element = cellRefs.value.get(key);
    if (element) {
      element.focus();
      element.select();
    }
  });
}

function handleKeyDown(event: KeyboardEvent, rowIndex: number, field: string) {
  if (!selectedJobData.value) return;

  const maxRow = selectedJobData.value.tasks.length - 1;
  const fields = ['actionType', 'context', 'payables'] as const;
  const currentFieldIndex = fields.indexOf(field as any);

  switch (event.key) {
    case 'Tab':
      event.preventDefault();
      if (event.shiftKey) {
        // Navigate backwards
        if (currentFieldIndex > 0) {
          focusCell(rowIndex, fields[currentFieldIndex - 1]);
        } else if (rowIndex > 0) {
          focusCell(rowIndex - 1, fields[fields.length - 1]);
        }
      } else {
        // Navigate forwards
        if (currentFieldIndex < fields.length - 1) {
          focusCell(rowIndex, fields[currentFieldIndex + 1]);
        } else if (rowIndex < maxRow) {
          focusCell(rowIndex + 1, fields[0]);
        }
      }
      break;

    case 'ArrowUp':
      if (rowIndex > 0) {
        event.preventDefault();
        focusCell(rowIndex - 1, field);
      }
      break;

    case 'ArrowDown':
      if (rowIndex < maxRow) {
        event.preventDefault();
        focusCell(rowIndex + 1, field);
      }
      break;

    case 'ArrowLeft':
      if (currentFieldIndex > 0) {
        event.preventDefault();
        focusCell(rowIndex, fields[currentFieldIndex - 1]);
      }
      break;

    case 'ArrowRight':
      if (currentFieldIndex < fields.length - 1) {
        event.preventDefault();
        focusCell(rowIndex, fields[currentFieldIndex + 1]);
      }
      break;

    case 'Enter':
      event.preventDefault();
      // Move to next row, same column
      if (rowIndex < maxRow) {
        focusCell(rowIndex + 1, field);
      }
      break;

    case 'Escape':
      event.preventDefault();
      focusedCell.value = null;
      (event.target as HTMLElement).blur();
      break;
  }
}

function registerCellRef(key: string, el: any) {
  if (el) {
    cellRefs.value.set(key, el);
  } else {
    cellRefs.value.delete(key);
  }
}

// Toggle payable editor popover
function togglePayableEditor(jobKey: string, taskIndex: number) {
  if (payablePopoverTarget.value?.jobKey === jobKey && payablePopoverTarget.value?.taskIndex === taskIndex) {
    payablePopoverTarget.value = null;
  } else {
    payablePopoverTarget.value = { jobKey, taskIndex };
  }
}

function closePayableEditor() {
  payablePopoverTarget.value = null;
}

// Save
async function save() {
  if (!payload.value) return;
  saving.value = true;
  savedCode.value = null;
  try {
    const newCode = await saveSession(payload.value);
    savedCode.value = newCode;
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save';
  } finally {
    saving.value = false;
  }
}

// Load session
onMounted(async () => {
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

// Helper to get action type display name
function actionTypeDisplayName(key: string): string {
  const parts = key.split(':');
  return parts[parts.length - 1];
}

// Helper to get payable type display name
function payableTypeDisplayName(key: string): string {
  const parts = key.split(':');
  return parts[parts.length - 1];
}
</script>

<template>
  <div class="h-screen flex flex-col bg-base-200">
    <!-- Header -->
    <div class="bg-base-100 border-b border-base-300 px-6 py-4 flex items-center justify-between shadow-sm">
      <div>
        <h1 class="text-2xl font-bold">Job Tasks Editor</h1>
        <p class="text-sm text-opacity-60 text-base-content mt-1">Edit job tasks like a spreadsheet</p>
      </div>
      <button @click="save" :disabled="saving" class="btn btn-primary gap-2">
        <span v-if="saving" class="loading loading-spinner loading-sm"></span>
        <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
          <path d="M7.707 10.293a1 1 0 10-1.414 1.414l3 3a1 1 0 001.414 0l3-3a1 1 0 00-1.414-1.414L11 11.586V6h5a2 2 0 012 2v7a2 2 0 01-2 2H4a2 2 0 01-2-2V8a2 2 0 012-2h5v5.586l-1.293-1.293zM9 4a1 1 0 012 0v2H9V4z" />
        </svg>
        {{ saving ? 'Saving...' : 'Save Changes' }}
      </button>
    </div>

    <!-- Success Alert -->
    <div v-if="savedCode" class="mx-6 mt-4">
      <div class="alert alert-success shadow-lg">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 shrink-0 stroke-current" fill="none" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div>
          <h3 class="font-bold">Changes saved!</h3>
          <div class="text-xs">Run in-game: <code class="font-mono bg-base-300 px-1 rounded">/jobs applyedits {{ savedCode }}</code></div>
        </div>
      </div>
    </div>

    <!-- Error Alert -->
    <div v-if="error" class="mx-6 mt-4">
      <div class="alert alert-error shadow-lg">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 shrink-0 stroke-current" fill="none" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{{ error }}</span>
      </div>
    </div>

    <!-- Main Content -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="payload" class="flex-1 flex flex-col overflow-hidden">
      <!-- Job Tabs -->
      <div class="bg-base-100 border-b border-base-300 px-6">
        <div class="tabs tabs-boxed bg-base-200 p-1 -mb-px">
          <button
            v-for="key in jobKeys"
            :key="key"
            @click="selectedJobKey = key"
            :class="[
              'tab tab-sm gap-2',
              selectedJobKey === key ? 'tab-active bg-base-100' : ''
            ]"
          >
            <span>{{ payload.jobs[key].displayName }}</span>
            <span class="badge badge-ghost badge-sm">{{ payload.jobs[key].tasks.length }}</span>
          </button>
        </div>
      </div>

      <!-- Spreadsheet Table -->
      <div v-if="selectedJobData" class="flex-1 overflow-auto p-6">
        <div class="bg-base-100 rounded-lg shadow-lg overflow-hidden">
          <table class="table table-zebra table-fixed w-full">
            <thead class="bg-base-300 sticky top-0 z-10">
              <tr>
                <th class="w-12 bg-base-300">#</th>
                <th class="w-48 bg-base-300">Action Type</th>
                <th class="bg-base-300">Context</th>
                <th class="w-64 bg-base-300">Payables</th>
                <th class="w-32 bg-base-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(task, index) in selectedJobData.tasks"
                :key="index"
                :class="{
                  'row-active': focusedCell?.rowIndex === index
                }"
                class="group hover:bg-opacity-50 hover:bg-base-200"
              >
                <!-- Row Number -->
                <td class="text-opacity-40 text-base-content font-mono text-sm text-center">
                  {{ index + 1 }}
                </td>

                <!-- Action Type -->
                <td class="p-0">
                  <select
                    :ref="el => registerCellRef(`${index}-actionType`, el)"
                    v-model="task.actionTypeKey"
                    @keydown="handleKeyDown($event, index, 'actionType')"
                    class="select select-ghost select-sm w-full h-full min-h-12 border-0 focus:bg-base-200 rounded-none"
                  >
                    <option v-for="at in payload.registeredActionTypes" :key="at" :value="at">
                      {{ actionTypeDisplayName(at) }}
                    </option>
                  </select>
                </td>

                <!-- Context -->
                <td class="p-0">
                  <input
                    :ref="el => registerCellRef(`${index}-context`, el)"
                    type="text"
                    v-model="task.contextKey"
                    @keydown="handleKeyDown($event, index, 'context')"
                    placeholder="minecraft:stone"
                    class="input input-ghost input-sm w-full h-full min-h-12 border-0 focus:bg-base-200 rounded-none"
                  />
                </td>

                <!-- Payables -->
                <td class="p-0 relative">
                  <button
                    :ref="el => registerCellRef(`${index}-payables`, el as any)"
                    @click="togglePayableEditor(selectedJobKey!, index)"
                    @keydown="handleKeyDown($event, index, 'payables')"
                    tabindex="0"
                    class="w-full h-full min-h-12 px-3 text-left hover:bg-opacity-50 hover:bg-base-200 focus:bg-base-200 transition-colors flex items-center gap-1 flex-wrap"
                  >
                    <span
                      v-for="(payable, pIndex) in task.payables"
                      :key="pIndex"
                      class="badge badge-ghost badge-sm gap-1"
                    >
                      <span class="font-medium">{{ payableTypeDisplayName(payable.type) }}</span>
                      <span class="text-opacity-60 text-base-content">{{ payable.amount }}</span>
                    </span>
                    <span v-if="task.payables.length === 0" class="text-opacity-40 text-base-content italic text-sm">
                      Add payables...
                    </span>
                  </button>

                  <!-- Payable Editor Popover -->
                  <div
                    v-if="payablePopoverTarget?.jobKey === selectedJobKey && payablePopoverTarget?.taskIndex === index"
                    class="absolute left-0 top-full mt-1 z-50 w-80 bg-base-100 rounded-lg shadow-2xl border border-base-300 overflow-hidden"
                    v-click-outside="closePayableEditor"
                  >
                    <div class="bg-base-300 px-3 py-2 border-b border-base-300 flex items-center justify-between">
                      <span class="text-sm font-medium">Edit Payables</span>
                      <button @click="closePayableEditor" class="btn btn-ghost btn-xs btn-circle">✕</button>
                    </div>
                    <div class="p-3 space-y-2 max-h-64 overflow-y-auto">
                      <div
                        v-for="(payable, pIndex) in task.payables"
                        :key="pIndex"
                        class="flex gap-2 items-center"
                      >
                        <select
                          :value="payable.type"
                          @change="updatePayableType(selectedJobKey!, index, pIndex, ($event.target as HTMLSelectElement).value)"
                          class="select select-bordered select-xs flex-1"
                        >
                          <option v-for="pt in payload.registeredPayableTypes" :key="pt" :value="pt">
                            {{ payableTypeDisplayName(pt) }}
                          </option>
                        </select>
                        <input
                          type="text"
                          :value="payable.amount"
                          @input="updatePayableAmount(selectedJobKey!, index, pIndex, ($event.target as HTMLInputElement).value)"
                          class="input input-bordered input-xs w-20"
                          placeholder="Amount"
                        />
                        <button
                          @click="deletePayable(selectedJobKey!, index, pIndex)"
                          class="btn btn-ghost btn-xs btn-circle text-error"
                          :disabled="task.payables.length === 1"
                          title="Delete payable"
                        >
                          ✕
                        </button>
                      </div>
                      <button
                        @click="addPayable(selectedJobKey!, index)"
                        class="btn btn-outline btn-xs btn-block gap-1"
                      >
                        <span>+ Add Payable</span>
                      </button>
                    </div>
                  </div>
                </td>

                <!-- Actions -->
                <td class="p-2">
                  <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      @click="duplicateTask(selectedJobKey!, index)"
                      class="btn btn-ghost btn-xs btn-square"
                      title="Duplicate row"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M7 9a2 2 0 012-2h6a2 2 0 012 2v6a2 2 0 01-2 2H9a2 2 0 01-2-2V9z" />
                        <path d="M5 3a2 2 0 00-2 2v6a2 2 0 002 2V5h8a2 2 0 00-2-2H5z" />
                      </svg>
                    </button>
                    <button
                      @click="deleteTask(selectedJobKey!, index)"
                      class="btn btn-ghost btn-xs btn-square text-error"
                      title="Delete row"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Empty State -->
              <tr v-if="selectedJobData.tasks.length === 0">
                <td colspan="5" class="text-center py-12 text-opacity-40 text-base-content">
                  <div class="flex flex-col items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                    </svg>
                    <span>No tasks yet. Add your first task below.</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Add Row Footer -->
          <div class="border-t border-base-300 p-2 bg-opacity-50 bg-base-200">
            <button
              @click="addTask(selectedJobKey!)"
              class="btn btn-sm btn-block btn-ghost gap-2"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
              </svg>
              Add Task Row
            </button>
          </div>
        </div>

        <!-- Keyboard Shortcuts Help -->
        <div class="mt-4 text-sm text-opacity-60 text-base-content">
          <p><strong>Keyboard shortcuts:</strong> Tab/Enter to navigate • Arrow keys to move • Escape to unfocus</p>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="bg-base-100 border-t border-base-300 px-6 py-3 text-xs text-opacity-40 text-base-content">
      <span v-if="payload">Session: {{ sessionCode }}</span>
      <span v-if="selectedJobData" class="ml-4">Tasks: {{ selectedJobData.tasks.length }}</span>
    </div>
  </div>
</template>

<style scoped>
.row-active {
  background-color: hsl(var(--b2) / 0.8);
}

.select-ghost:focus, .input-ghost:focus {
  @apply outline-none;
}

/* Click outside directive */
[type-v-click-outside] {
  display: none;
}
</style>

<!-- Click outside directive for popover -->
<script lang="ts">
import { Directive } from 'vue';

const vClickOutside: Directive = {
  mounted(el, binding) {
    el._clickOutside = (event: MouseEvent) => {
      if (!(el === event.target || el.contains(event.target as Node))) {
        binding.value(event);
      }
    };
    document.addEventListener('click', el._clickOutside);
  },
  unmounted(el) {
    document.removeEventListener('click', el._clickOutside);
  }
};
</script>