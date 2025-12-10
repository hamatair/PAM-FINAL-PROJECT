pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ TAMBAHKAN REPOSITORI JITPACK SEBAGAI JARING PENGAMAN
        // Supabase biasanya tidak memerlukannya, tetapi ini akan mencegah masalah resolver
        // jika ada dependensi Ktor atau KMP lainnya yang memerlukan ini.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "pam_1"
include(":app")
 