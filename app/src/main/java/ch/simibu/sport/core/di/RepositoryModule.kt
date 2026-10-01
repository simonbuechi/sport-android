package ch.simibu.sport.core.di

import ch.simibu.sport.data.repository.AuthRepositoryImpl
import ch.simibu.sport.data.repository.ExerciseRepositoryImpl
import ch.simibu.sport.data.repository.TemplateRepositoryImpl
import ch.simibu.sport.data.repository.UserProfileRepositoryImpl
import ch.simibu.sport.data.repository.WorkoutRepositoryImpl
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.domain.repository.ExerciseRepository
import ch.simibu.sport.domain.repository.TemplateRepository
import ch.simibu.sport.domain.repository.UserProfileRepository
import ch.simibu.sport.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: ExerciseRepositoryImpl
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        impl: TemplateRepositoryImpl
    ): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository
}
