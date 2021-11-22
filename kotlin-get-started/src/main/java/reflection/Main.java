package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Main {

    public static void main(String[] args) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {

        Constructor<Container<ItemOne>> constructor = (Constructor<Container<ItemOne>>) Container.class.getConstructors()[0];

        ItemTwo item = ItemTwo.class.getConstructor().newInstance();

        Container<ItemOne> containerOneReflection = constructor.newInstance(item);

        System.out.println("containerOneReflection.item is NOT ItemOne " + (containerOneReflection.item instanceof ItemOne));
    }

    interface Item {
    }

    public static class ItemOne implements Item {
        public ItemOne() {
        }
    }

    public static class Container<T extends Item> {
        final T item;

        public Container(T item) {
            this.item = item;
        }
    }

    public static class ItemTwo implements Item {
        public ItemTwo() {
        }
    }
}
