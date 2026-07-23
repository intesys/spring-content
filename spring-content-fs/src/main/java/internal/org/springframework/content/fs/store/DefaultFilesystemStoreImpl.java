package internal.org.springframework.content.fs.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.commons.store.UnsetContentParams.Disposition;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.Condition;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Filesystem-backed {@link ContentStore} implementation.
 *
 * <p>Writes are durable: {@link #setContent} copies the content through a {@link FileChannel}
 * this store owns and {@code fsync}s the blob ({@link FileChannel#force(boolean) force(true)})
 * before the call returns, then best-effort {@code fsync}s the containing directory. A
 * successful {@code setContent} therefore implies the bytes have reached stable storage; any
 * flush/{@code fsync}/close error surfaces as a {@link StoreAccessException} rather than a
 * silent success (relevant on filesystems such as NFS that defer write errors to close).
 *
 * @author marcobelligoli
 */
@Transactional(readOnly = true)
public class DefaultFilesystemStoreImpl<S, SID extends Serializable>
		implements Store<SID>, AssociativeStore<S, SID>, ContentStore<S, SID>,
		org.springframework.content.commons.store.ContentStore<S, SID> {

	private static Log logger = LogFactory.getLog(DefaultFilesystemStoreImpl.class);

	private FileSystemResourceLoader loader;
	private PlacementService placer;
	private FileService fileService;
    private MappingContext mappingContext/* = new MappingContext("/", ".")*/;

	public DefaultFilesystemStoreImpl(FileSystemResourceLoader loader, MappingContext mappingContext, PlacementService conversion, FileService fileService) {
		this.loader = loader;
		this.placer = conversion;
		this.fileService = fileService;
		this.mappingContext = mappingContext;
		if (this.mappingContext == null) {
		    this.mappingContext = new MappingContext("/", ".");
		}
	}

	@Override
	public Resource getResource(SID id) {
		String location = placer.convert(id, String.class);
		Resource resource = loader.getResource(location);
		return resource;
	}

	@Override
	public Resource getResource(S entity) {
		Resource resource = null;
		if (placer.canConvert(entity.getClass(), String.class)) {
			String location = placer.convert(entity, String.class);
			resource = loader.getResource(location);
			if (resource != null) {
				return resource;
			}
		}

		SID contentId = (SID) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
		if (contentId != null) {
			return getResource(contentId);
		}

		return null;
	}

    @Override
    public Resource getResource(S entity, PropertyPath propertyPath) {
		return this.getResource(entity, propertyPath, GetResourceParams.builder().build());
    }

	@Override
	public Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params) {
		ContentProperty contentProperty = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
		if (contentProperty == null) {
			throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
		}

		SID contentId = (SID) contentProperty.getContentId(entity);
		if (contentId == null) {
			return null;
		}
		return getResource(contentId);
	}

	@Override
	public Resource getResource(S entity, PropertyPath propertyPath, org.springframework.content.commons.repository.GetResourceParams params) {
		ContentProperty contentProperty = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
		if (contentProperty == null) {
			throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
		}

		SID contentId = (SID) contentProperty.getContentId(entity);
		if (contentId == null) {
			return null;
		}
		return getResource(contentId);
	}

	@Override
	public void associate(S entity, SID id) {
		BeanUtils.setFieldWithAnnotation(entity, ContentId.class, id.toString());
	}

    @Override
    public void associate(S entity, PropertyPath propertyPath, SID id) {

        setContentId(entity, propertyPath, id, null);
    }

	@Override
	public void unassociate(S entity) {
		BeanUtils.setFieldWithAnnotationConditionally(entity, ContentId.class, null,
				new Condition() {
					@Override
					public boolean matches(Field field) {
						for (Annotation annotation : field.getAnnotations()) {
							if ("jakarta.persistence.Id".equals(
									annotation.annotationType().getCanonicalName())
									|| "org.springframework.data.annotation.Id"
									.equals(annotation.annotationType()
											.getCanonicalName())) {
								return false;
							}
						}
						return true;
					}
				});
	}

    @Override
    public void unassociate(S entity, PropertyPath propertyPath) {

        setContentId(entity, propertyPath, null, new org.springframework.content.commons.mappingcontext.Condition() {
            @Override
            public boolean matches(TypeDescriptor descriptor) {
                for (Annotation annotation : descriptor.getAnnotations()) {
                    if ("javax.persistence.Id".equals(
                            annotation.annotationType().getCanonicalName())
                            || "org.springframework.data.annotation.Id"
                            .equals(annotation.annotationType()
                                    .getCanonicalName())) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

	@Override
	@Transactional
	public S setContent(S entity, InputStream content) {
		Object contentId = BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (contentId == null) {

            Serializable newId = UUID.randomUUID().toString();

            Object convertedId = convertToExternalContentIdType(entity, newId);

            BeanUtils.setFieldWithAnnotation(entity, ContentId.class, convertedId);
        }

        Resource resource = this.getResource(entity);
        if (resource == null) {
            return entity;
        }

		try {
			if (resource.exists() == false) {
				File resourceFile = resource.getFile();
				File parent = resourceFile.getParentFile();
				this.fileService.mkdirs(parent);
			}
			if (resource instanceof WritableResource) {
				writeDurably(resource, content);
			}

		} catch (IOException e) {
			logger.error(format("Unexpected io error setting content for entity %s", entity), e);
			throw new StoreAccessException(format("Setting content for entity %s", entity), e);
		}

		try {
			BeanUtils.setFieldWithAnnotation(entity, ContentLength.class,
					resource.contentLength());
		}
		catch (IOException e) {
			logger.error(format(
					"Unexpected error setting content length for content for resource %s",
					resource.toString()), e);
		}

		return entity;
	}

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, InputStream content) {
		return this.setContent(property, propertyPath, content, -1L);
    }

	@Transactional
	@Override
	public S setContent(S property, PropertyPath propertyPath, InputStream content, long contentLen) {
		return this.setContent(property, propertyPath, content, SetContentParams.builder().contentLength(contentLen).build());
	}

	@Override
	public S setContent(S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.repository.SetContentParams params) {
		int ordinal = params.getDisposition().ordinal();
		SetContentParams params1 = SetContentParams.builder()
				.contentLength(params.getContentLength())
				.overwriteExistingContent(params.isOverwriteExistingContent())
				.disposition(org.springframework.content.commons.store.SetContentParams.ContentDisposition.values()[ordinal])
				.build();
		return this.setContent(entity, propertyPath, content, params1);
	}

	@Transactional
	@Override
	public S setContent(S property, PropertyPath propertyPath, InputStream content, SetContentParams params) {

		ContentProperty contentProperty = this.mappingContext.getContentProperty(property.getClass(), propertyPath.getName());
		if (contentProperty == null) {
			throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
		}

		Object contentId = contentProperty.getContentId(property);
		if (contentId == null || params.getDisposition().equals(org.springframework.content.commons.store.SetContentParams.ContentDisposition.CreateNew)) {

			Serializable newId = UUID.randomUUID().toString();

			Object convertedId = placer.convert(
					newId,
					TypeDescriptor.forObject(newId),
					contentProperty.getContentIdType(property));

			contentProperty.setContentId(property, convertedId, null);
		}

		Resource resource = this.getResource(property, propertyPath);
		if (resource == null) {
			return property;
		}

		try {
			if (resource.exists() == false) {
				File resourceFile = resource.getFile();
				File parent = resourceFile.getParentFile();
				this.fileService.mkdirs(parent);
			}
			if (resource instanceof WritableResource) {
				writeDurably(resource, content);
			}
		} catch (IOException e) {
			logger.error(format("Unexpected io error setting content for entity %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}

		try {
			long len = params.getContentLength();
			if (len == -1L) {
				len = resource.contentLength();
			}
			contentProperty.setContentLength(property, len);
		}
		catch (IOException e) {
			logger.error(format(
					"Unexpected error setting content length for content for resource %s",
					resource.toString()), e);
		}

		return property;
	}

	@Transactional
	@Override
	public S setContent(S property, Resource resourceContent) {
		try {
			return this.setContent(property, resourceContent.getInputStream());
		} catch (IOException e) {
			logger.error(format("Unexpected error setting content for entity %s", property), e);
			throw new StoreAccessException(format("Setting content for entity %s", property), e);
		}
	}

    @Transactional
    @Override
    public S setContent(S property, PropertyPath propertyPath, Resource resourceContent) {
        try {
            return this.setContent(property, propertyPath, resourceContent.getInputStream());
        } catch (IOException e) {
            logger.error(format("Unexpected error setting content for entity %s", property), e);
            throw new StoreAccessException(format("Setting content for entity %s", property), e);
        }
    }

	@Override
	@Transactional
	public InputStream getContent(S entity) {
		if (entity == null)
			return null;

		Resource resource = getResource(entity);

		try {
			if (resource != null && resource.exists()) {
				return resource.getInputStream();
			}
		}
		catch (IOException e) {
			logger.error(format("Unexpected error getting content for entity %s", entity), e);
			throw new StoreAccessException(format("Getting content for entity %s", entity), e);
		}

		return null;
	}

    @Transactional
    @Override
    public InputStream getContent(S property, PropertyPath propertyPath) {

        if (property == null)
            return null;

        Resource resource = getResource(property, propertyPath);

        try {
            if (resource != null && resource.exists()) {
                return resource.getInputStream();
            }
        }
        catch (IOException e) {
            logger.error(format("Unexpected error getting content for entity %s", property), e);
            throw new StoreAccessException(format("Getting content for entity %s", property), e);
        }

        return null;
    }

	@Override
	@Transactional
	public S unsetContent(S entity) {

		if (entity == null)
			return entity;

		Resource resource = getResource(entity);

		if (resource != null && resource.exists() && resource instanceof DeletableResource) {
			try {
				((DeletableResource) resource).delete();
			} catch (IOException e) {
				logger.warn(format("Unable to get file for resource %s", resource));
			}
		}

		// reset content fields
		unassociate(entity);

		Class<?> contentLenType = BeanUtils.getFieldWithAnnotationType(entity, ContentLength.class);
		if (contentLenType != null) {
			BeanUtils.setFieldWithAnnotation(entity, ContentLength.class, BeanUtils.getDefaultValueForType(contentLenType));
		}

		return entity;
	}

    @Transactional
    @Override
    public S unsetContent(S entity, PropertyPath propertyPath) {
        return unsetContent(entity, propertyPath, UnsetContentParams.builder().disposition(Disposition.Remove).build());
    }

	@Transactional
	@Override
	public S unsetContent(S entity, PropertyPath propertyPath, org.springframework.content.commons.repository.UnsetContentParams params) {
		int ordinal = params.getDisposition().ordinal();
		return unsetContent(entity, propertyPath, UnsetContentParams.builder().disposition(Disposition.values()[ordinal]).build());
	}

	@Transactional
	@Override
	public S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params) {
		ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
		if (property == null) {
			throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
		}

		if (entity == null)
			return entity;

		Resource resource = getResource(entity, propertyPath);

		if (resource != null && resource.exists() && resource instanceof DeletableResource && params.getDisposition().equals(Disposition.Remove)) {
			try {
				((DeletableResource) resource).delete();
			} catch (IOException e) {
				logger.warn(format("Unable to get file for resource %s", resource));
			}
		}

		// reset content fields
		if (resource != null) {
			unassociate(entity, propertyPath);

			property.setContentLength(entity, BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
		}
		return entity;
	}

	/**
	 * Durably copies {@code content} into the blob backing {@code resource}.
	 *
	 * <p>The store owns the write channel (opened deterministically from the resource's
	 * {@link Resource#getFile() file}) rather than relying on the {@link WritableResource}'s
	 * {@code OutputStream}, whose concrete type — and therefore whether an {@code fsync} handle
	 * is even reachable — is not guaranteed across Spring/JDK versions. After the full copy the
	 * blob is {@link FileChannel#force(boolean) fsync}'d and only then closed (via
	 * {@code try}-with-resources) so flush/{@code fsync}/close {@link IOException}s propagate to
	 * the caller instead of being swallowed. The containing directory is then {@code fsync}'d
	 * best-effort (see {@link #syncDirectory(File)}).
	 *
	 * <p>The supplied {@code content} stream is <em>not</em> closed here — closing it remains the
	 * caller's responsibility, unchanged from the previous behaviour.
	 *
	 * @throws IOException if opening, writing, {@code fsync}ing, or closing the blob fails; the
	 *         caller maps this to {@link StoreAccessException}.
	 */
	private void writeDurably(Resource resource, InputStream content) throws IOException {
		File file = resource.getFile();
		try (FileChannel channel = openChannel(file.toPath())) {
			copy(content, channel);
			channel.force(true);
		}
		syncDirectory(file.getParentFile());
	}

	/**
	 * Opens the blob write channel with {@code CREATE, WRITE, TRUNCATE_EXISTING}. Package-private
	 * so tests can decorate/fault-inject the returned channel.
	 */
	FileChannel openChannel(Path path) throws IOException {
		return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	private void copy(InputStream in, FileChannel out) throws IOException {
		byte[] buffer = new byte[8192];
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		int read;
		while ((read = in.read(buffer)) != -1) {
			bb.position(0).limit(read);
			while (bb.hasRemaining()) {
				out.write(bb);
			}
		}
	}

	/**
	 * Best-effort {@code fsync} of the directory holding a newly-created blob so its directory
	 * entry survives a crash. Some platforms/filesystems (notably Windows) reject opening or
	 * {@code fsync}ing a directory; such a failure is logged and swallowed and MUST NOT by itself
	 * fail an otherwise-durable blob write.
	 */
	private void syncDirectory(File directory) {
		if (directory == null) {
			return;
		}
		try (FileChannel dirChannel = FileChannel.open(directory.toPath(), StandardOpenOption.READ)) {
			dirChannel.force(true);
		}
		catch (IOException e) {
			logger.debug(format("Best-effort directory fsync failed for %s", directory), e);
		}
	}

	private Object convertToExternalContentIdType(S property, Object contentId) {
		if (placer.canConvert(TypeDescriptor.forObject(contentId),
				TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
						ContentId.class)))) {
			contentId = placer.convert(contentId, TypeDescriptor.forObject(contentId),
					TypeDescriptor.valueOf(BeanUtils.getFieldWithAnnotationType(property,
							ContentId.class)));
			return contentId;
		}
		return contentId.toString();
	}

    private void setContentId(S entity, PropertyPath propertyPath, SID contentId, org.springframework.content.commons.mappingcontext.Condition condition) {

        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(propertyPath, "propertyPath must not be null");

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.getName()));
        }
        property.setContentId(entity, contentId, condition);
    }
}