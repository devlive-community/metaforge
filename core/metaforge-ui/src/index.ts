import type { App } from 'vue'
import MfAlert from './components/MfAlert.vue'
import MfAvatar from './components/MfAvatar.vue'
import MfBadge from './components/MfBadge.vue'
import MfBreadcrumb from './components/MfBreadcrumb.vue'
import MfBreadcrumbItem from './components/MfBreadcrumbItem.vue'
import MfButton from './components/MfButton.vue'
import MfCard from './components/MfCard.vue'
import MfCheckbox from './components/MfCheckbox.vue'
import MfCheckboxGroup from './components/MfCheckboxGroup.vue'
import MfConfigProvider from './components/MfConfigProvider.vue'
import MfDivider from './components/MfDivider.vue'
import MfDrawer from './components/MfDrawer.vue'
import MfDropdown from './components/MfDropdown.vue'
import MfForm from './components/MfForm.vue'
import MfFormItem from './components/MfFormItem.vue'
import MfInput from './components/MfInput.vue'
import MfInputNumber from './components/MfInputNumber.vue'
import MfMenu from './components/MfMenu.vue'
import MfMenuItem from './components/MfMenuItem.vue'
import MfModal from './components/MfModal.vue'
import MfPagination from './components/MfPagination.vue'
import MfPopover from './components/MfPopover.vue'
import MfRadio from './components/MfRadio.vue'
import MfRadioGroup from './components/MfRadioGroup.vue'
import MfResult from './components/MfResult.vue'
import MfSelect from './components/MfSelect.vue'
import MfSkeleton from './components/MfSkeleton.vue'
import MfSpace from './components/MfSpace.vue'
import MfSpinner from './components/MfSpinner.vue'
import MfSubMenu from './components/MfSubMenu.vue'
import MfSwitch from './components/MfSwitch.vue'
import MfTabPane from './components/MfTabPane.vue'
import MfTable from './components/MfTable.vue'
import MfTabs from './components/MfTabs.vue'
import MfTag from './components/MfTag.vue'
import MfTextarea from './components/MfTextarea.vue'
import MfTooltip from './components/MfTooltip.vue'
import MfTree from './components/MfTree.vue'
import MfUpload from './components/MfUpload.vue'

export const version = '2026.0.0'

export const components = {
    MfAlert,
    MfAvatar,
    MfBadge,
    MfBreadcrumb,
    MfBreadcrumbItem,
    MfButton,
    MfCard,
    MfCheckbox,
    MfCheckboxGroup,
    MfConfigProvider,
    MfDivider,
    MfDrawer,
    MfDropdown,
    MfForm,
    MfFormItem,
    MfInput,
    MfInputNumber,
    MfMenu,
    MfMenuItem,
    MfModal,
    MfPagination,
    MfPopover,
    MfRadio,
    MfRadioGroup,
    MfResult,
    MfSelect,
    MfSkeleton,
    MfSpace,
    MfSpinner,
    MfSubMenu,
    MfSwitch,
    MfTabPane,
    MfTable,
    MfTabs,
    MfTag,
    MfTextarea,
    MfTooltip,
    MfTree,
    MfUpload,
}

export type { MfSelectOption } from './components/common'
export type { MfTableColumn } from './components/MfTable.vue'
export type { MfTreeNode } from './components/MfTree.vue'
export type { MfFormRule, MfFormRules } from './components/MfForm.vue'
export type { MfUploadFile } from './components/MfUpload.vue'

export function install(app: App): void {
    for (const [name, component] of Object.entries(components)) {
        app.component(name, component)
    }
}

export {
    MfAlert,
    MfAvatar,
    MfBadge,
    MfBreadcrumb,
    MfBreadcrumbItem,
    MfButton,
    MfCard,
    MfCheckbox,
    MfCheckboxGroup,
    MfConfigProvider,
    MfDivider,
    MfDrawer,
    MfDropdown,
    MfForm,
    MfFormItem,
    MfInput,
    MfInputNumber,
    MfMenu,
    MfMenuItem,
    MfModal,
    MfPagination,
    MfPopover,
    MfRadio,
    MfRadioGroup,
    MfResult,
    MfSelect,
    MfSkeleton,
    MfSpace,
    MfSpinner,
    MfSubMenu,
    MfSwitch,
    MfTabPane,
    MfTable,
    MfTabs,
    MfTag,
    MfTextarea,
    MfTooltip,
    MfTree,
    MfUpload,
}

export * from './theme/theme'
export * from './theme/message'

export default { install, version }
