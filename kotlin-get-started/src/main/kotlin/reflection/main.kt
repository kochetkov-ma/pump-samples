package reflection

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KFunction

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val constructor: KFunction<Container<ItemOne>> = Container::class.constructors.first() as KFunction<Container<ItemOne>>

    val item: ItemTwo = ItemTwo::class.constructors.first().call()

    val containerOneReflection: Container<ItemOne> = constructor.call(item)

    println("containerOneReflection.item is NOT ItemOne " + (containerOneReflection.item !is ItemOne).also(::assert))

    println("containerOneReflection.item real class: " + containerOneReflection.item::class)

    // Для типа Container поучить generic тип невозможно.
    // Максиму, что можно сделать это получить родительский тип генерика Item
    val genericClass = containerOneReflection.javaClass.typeParameters[0].bounds[0]

    println("containerOneReflection generic: $genericClass")

    // Но если есть класс, который наследует Container?
    // Максиму, что можно сделать это получить родительский тип генерика Item
    val containerExt = ContainerExt(ItemTwo())
    val genericExtClass = (containerExt.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>

    println("containerExt generic: $genericExtClass")
}

interface Item
class ItemOne : Item
class ItemTwo : Item
open class Container<T : Item>(val item: T)
class ContainerExt(item: ItemTwo) : Container<ItemTwo>(item)

