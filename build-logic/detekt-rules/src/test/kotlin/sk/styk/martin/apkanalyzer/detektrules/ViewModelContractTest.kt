package sk.styk.martin.apkanalyzer.detektrules

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ViewModelContractTest {

    private val rule = ViewModelContract(Config.empty)

    @Test
    fun `Given a ViewModel following the contract When linting Then nothing is reported`() {
        val code = """
            @HiltViewModel(assistedFactory = AppDetailViewModel.Factory::class)
            internal class AppDetailViewModel @AssistedInject constructor(
                @Assisted private val packageName: String,
                private val repository: AppDetailRepository,
            ) : ViewModel() {

                private val eventChannel = Channel<AppDetailEvent>(Channel.BUFFERED)
                val events = eventChannel.receiveAsFlow()

                val state: StateFlow<AppDetailState>
                    field = MutableStateFlow(AppDetailState())

                init {
                    load()
                }

                fun onAction(action: AppDetailAction) {
                    when (action) {
                        AppDetailAction.NavigateBack -> eventChannel.trySend(AppDetailEvent.NavigateBack)
                    }
                }

                override fun onCleared() = Unit

                private fun load() = Unit

                @AssistedFactory
                interface Factory {
                    fun create(packageName: String): AppDetailViewModel
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a state built with stateIn and no declared type When linting Then nothing is reported`() {
        val code = """
            @HiltViewModel
            internal class SettingsViewModel @Inject constructor(
                private val repository: SettingsRepository,
            ) : ViewModel() {

                val state = repository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }

    @Test
    fun `Given a public method besides onAction When linting Then it is reported`() {
        val code = """
            internal class SettingsViewModel : ViewModel() {

                fun onAction(action: SettingsAction) = Unit

                fun refresh() = Unit
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Make 'refresh' private"
    }

    @Test
    fun `Given state exposed as a plain Flow When linting Then it is reported`() {
        val code = """
            internal class SettingsViewModel : ViewModel() {

                val state: Flow<SettingsState> = flowOf(SettingsState())
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Make 'state' private"
    }

    @Test
    fun `Given a HiltViewModel extending a base class When linting Then it is reported`() {
        val code = """
            @HiltViewModel
            internal class SettingsViewModel @Inject constructor() : BaseViewModel()
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Extend ViewModel() directly"
    }

    @Test
    fun `Given events sent through a MutableSharedFlow When linting Then it is reported`() {
        val code = """
            internal class SettingsViewModel : ViewModel() {

                private val eventFlow = MutableSharedFlow<SettingsEvent>()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Channel<Event>(Channel.BUFFERED)"
    }

    @Test
    fun `Given an unbuffered event channel When linting Then it is reported`() {
        val code = """
            internal class SettingsViewModel : ViewModel() {

                private val eventChannel = Channel<SettingsEvent>()
            }
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "Create the event channel with Channel.BUFFERED"
    }

    @Test
    fun `Given a ViewModel injecting Navigator When linting Then it is reported`() {
        val code = """
            import sk.styk.martin.apkanalyzer.core.navigation.Navigator

            internal class SettingsViewModel(
                private val navigator: Navigator,
            ) : ViewModel()
        """.trimIndent()

        val findings = rule.lint(code)

        findings shouldHaveSize 1
        findings.single().message shouldContain "never touches Navigator"
    }

    @Test
    fun `Given a Navigator import in a file without a ViewModel When linting Then nothing is reported`() {
        val code = """
            import sk.styk.martin.apkanalyzer.core.navigation.Navigator

            fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) = Unit
        """.trimIndent()

        val findings = rule.lint(code)

        findings.shouldBeEmpty()
    }
}
