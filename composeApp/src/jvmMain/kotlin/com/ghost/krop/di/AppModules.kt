package com.ghost.krop.di

import com.ghost.krop.repository.ImageRepository
import com.ghost.krop.repository.annotations.AnnotationManager
import com.ghost.krop.repository.annotations.AnnotationRepository
import com.ghost.krop.repository.settings.SettingsManager
import com.ghost.krop.repository.settings.SettingsRepository
import com.ghost.krop.viewModel.annotator.AnnotatorViewModel
import com.ghost.krop.viewModel.image.ImageViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val applicationScope = CoroutineScope(
    SupervisorJob() + Dispatchers.IO
)

val repositoryModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single<SettingsManager> { SettingsManager() }
    single {
        SettingsRepository(
            settingsManager = get(),
            scope = applicationScope,
        )
    }

    single<AnnotationManager> { AnnotationManager() }
    single<AnnotationRepository> {
        AnnotationRepository(
            annotationManager = get(),
            scope = applicationScope,
        )
    }

    single<ImageRepository> { ImageRepository(ioDispatcher = get()) }
}


// ViewModel module
val viewModelModule = module {
    factory<ImageViewModel> { params ->
        ImageViewModel(
            imageRepository = get(),
            settingsRepository = get()
        )
    }
    factory<AnnotatorViewModel> { params ->
        AnnotatorViewModel(
            settingsRepository = get(),
            annotationRepository = get(),
        )
    }

}

val appModule = module {
    includes(
        repositoryModule,
        viewModelModule
    )
}