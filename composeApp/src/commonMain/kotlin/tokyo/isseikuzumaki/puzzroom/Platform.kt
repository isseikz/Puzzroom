package tokyo.isseikuzumaki.puzzroom

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform