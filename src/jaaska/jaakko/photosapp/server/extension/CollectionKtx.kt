package jaaska.jaakko.photosapp.server.extension

fun <E> Collection<E>.contains(evaluator: (E) -> Boolean): Boolean = count { evaluator(it) } > 0