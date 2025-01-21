package cn.co.a;

import com.aristoco.core.annotation.Component;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

/**
 * @author chenguowei
 * @date 2024/6/21
 * @description
 **/
@Component
public class DProvider implements Provider<D> {
    /**
     * Provides an instance of {@code T}.
     *
     * @throws OutOfScopeException when an attempt is made to access a scoped object while the scope
     *                             in question is not currently active
     * @throws ProvisionException  if an instance cannot be provided. Such exceptions include messages
     *                             and throwables to describe why provision failed.
     */
    @Override
    public D get() {
        D d = new D();
        d.setIndex(100);
        return d;
    }
}
