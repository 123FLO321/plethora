package org.squiddev.plethora.api.meta;

import org.squiddev.plethora.api.method.IPartialContext;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * An interface-based version of {@link IMetaProvider}. One consumes the object directly, rather than needing
 * to use {@link IPartialContext#getTarget()}.
 *
 * @param <T> The type of object this provider handles.
 */
public interface SimpleMetaProvider<T> extends IMetaProvider<T> {
	@Nonnull
	@Override
	default Map<Object, Object> getMeta(@Nonnull IPartialContext<T> context) {
		return getMeta(context.getTarget());
	}

	@Nonnull
	Map<Object, Object> getMeta(@Nonnull T context);
}
