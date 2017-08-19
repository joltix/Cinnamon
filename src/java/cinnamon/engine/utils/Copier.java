package cinnamon.engine.utils;

/**
 * <p>Allows objects to copy the properties of another. Implementations are not required to duplicate the totality of a
 * given object's fields nor to subclass or be <tt>T</tt> itself.</p>
 *
 * <p>This is unlike <tt>clone()</tt> in that there is no expectation for a <tt>Copier</tt> that has copied an
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
     * @param object to copy.
     * @throws ClassCastException if the given object's class does not match the intended copy target.
     * @throws NullPointerException if the given object is null.
     * @throws IllegalArgumentException if something about the given object prevents copying.
     */
    void copy(T object);
}