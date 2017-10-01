package cinnamon.engine.utils;

/**
 * <p>Allows objects to copy the properties of another. Implementations are not required to duplicate the totality of a
 * given object's fields nor to subclass or be {@code T} itself.</p>
 *
 * <p>This is unlike {@code clone()} in that there is no expectation for a {@code Copier} that has copied an
 * object to cause either of the following expressions to evaluate as true: <code>copier.equals(copiedObject)</code>
 * or <code>copier.getClass() == copiedObj.getClass()</code>. Furthermore, object creation is separated from copying
 * fields. The intent is not so much to create identical objects as it is to provide a more flexible and succinct
 * method of bulk copying members between objects.
 *
 * @param <T> the type to copy.
 */
public interface Copier<T>
{
    /**
     * <p>Copies properties of a given object.</p>
     *
     * <p>A {@code ClassCastException} may be thrown if the intended object class to copy is not the exact
     * same as the class of the given object. This extends to super classes (e.g. if the declared target type is a
     * sub class of the actual given object, implementations may throw the exception).</p>
     *
     * @param object to copy.
     * @throws ClassCastException if the given object's class does not match the intended copy target.
     * @throws NullPointerException if the given object is null.
     * @throws IllegalArgumentException if something about the given object prevents copying.
     */
    void copy(T object);
}