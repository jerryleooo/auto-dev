package cc.unitmesh.devti.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.ide.actions.runAnything.RunAnythingPopupUI
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class WriteTestService : LazyExtensionInstance<WriteTestService>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementation")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? = implementationClass
    abstract fun runConfigurationClass(project: Project): Class<out RunProfile>
    abstract fun isApplicable(element: PsiElement): Boolean
    abstract fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext?
    abstract fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext>

    fun runTest(project: Project, virtualFile: VirtualFile) {
        val runManager = RunManager.getInstance(project)
        val allConfigurationsList = runManager.allConfigurationsList
        val testConfig = allConfigurationsList.firstOrNull {
            val runConfigureClass = runConfigurationClass(project)
            it.name == virtualFile.nameWithoutExtension && (it.javaClass == runConfigureClass)
        }

        if (testConfig == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        val configurationSettings =
            runManager.findConfigurationByTypeAndName(testConfig.getType(), testConfig.name)

        if (configurationSettings == null) {
            log.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        log.info("configurationSettings: $configurationSettings")
        runManager.selectedConfiguration = configurationSettings

        val executor: Executor = RunAnythingPopupUI.getExecutor()
        ExecutorRegistryImpl.RunnerHelper.run(
            project,
            testConfig,
            configurationSettings,
            DataContext.EMPTY_CONTEXT,
            executor
        )
    }

    companion object {
        val log = logger<WriteTestService>()
        private val EP_NAME: ExtensionPointName<WriteTestService> =
            ExtensionPointName.create("cc.unitmesh.testContextProvider")

        fun context(psiElement: PsiElement): WriteTestService? {
            val lang = psiElement.language.displayName.lowercase()
            val extensionList = EP_NAME.extensionList
            val testServices = filterByLang(extensionList, lang)

            val service = if (testServices.isNotEmpty()) {
                testServices.first()
            } else {
                // if lang == "TypeScript JSX", we just use TypeScript
                val firstPartLang = lang.split(" ")[0]
                val partLang = filterByLang(extensionList, firstPartLang)
                if (partLang.isNotEmpty()) {
                    partLang[0]
                } else {
                    logger<WriteTestService>().warn("No context prompter found for language $lang, will use default")
                    return null
                }
            }

            return service
        }

        private fun filterByLang(
            extensionList: List<WriteTestService>,
            langLowercase: String
        ): List<WriteTestService> {
            val contextPrompter = extensionList.filter {
                it.language?.lowercase() == langLowercase
            }

            return contextPrompter
        }
    }
}