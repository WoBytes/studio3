/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common;

import java.net.URI;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;

import com.aptana.core.resources.AbstractUniformResource;
import com.aptana.core.resources.IMarkerConstants;
import com.aptana.core.resources.IUniformResource;
import com.aptana.core.resources.IUniformResourceChangeEvent;
import com.aptana.core.resources.IUniformResourceChangeListener;
import com.aptana.core.resources.IUniformResourceMarker;
import com.aptana.core.resources.MarkerUtils;

public class ExternalFileAnnotationModel extends AbstractMarkerAnnotationModel
{

	private IUniformResource resource;
	private IUniformResourceChangeListener resourceChangeListener;

	public ExternalFileAnnotationModel(IPath file)
	{
		resource = new DefaultUniformResource(file);
		resourceChangeListener = new ResourceChangeListener();
	}

	@Override
	protected IMarker[] retrieveMarkers() throws CoreException
	{
		return MarkerUtils.findMarkers(resource, IMarkerConstants.PROBLEM_MARKER, true);
	}

	@Override
	protected void deleteMarkers(final IMarker[] markers) throws CoreException
	{
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable()
		{
			public void run(IProgressMonitor monitor) throws CoreException
			{
				for (int i = 0; i < markers.length; ++i)
				{
					markers[i].delete();
				}
			}
		}, null, IWorkspace.AVOID_UPDATE, null);
	}

	@Override
	protected void listenToMarkerChanges(boolean listen)
	{
		if (listen)
		{
			MarkerUtils.addResourceChangeListener(resourceChangeListener);
		}
		else
		{
			MarkerUtils.removeResourceChangeListener(resourceChangeListener);
		}
	}

	@Override
	protected boolean isAcceptable(IMarker marker)
	{
		return (marker instanceof IUniformResourceMarker)
				&& resource.equals(((IUniformResourceMarker) marker).getUniformResource());
	}

	/**
	 * Updates this model to the given marker deltas.
	 * 
	 * @param markerDeltas
	 *            the array of marker deltas
	 */
	protected void update(IMarkerDelta[] markerDeltas)
	{
		if (markerDeltas.length == 0)
		{
			return;
		}

		for (IMarkerDelta delta : markerDeltas)
		{
			switch (delta.getKind())
			{
				case IResourceDelta.ADDED:
					addMarkerAnnotation(delta.getMarker());
					break;
				case IResourceDelta.REMOVED:
					removeMarkerAnnotation(delta.getMarker());
					break;
				case IResourceDelta.CHANGED:
					modifyMarkerAnnotation(delta.getMarker());
					break;
				default:
					break;
			}
		}
		fireModelChanged();
	}

	private class DefaultUniformResource extends AbstractUniformResource
	{

		private URI uri;

		public DefaultUniformResource(IPath path)
		{
			uri = URIUtil.toURI(path);
		}

		public URI getURI()
		{
			return uri;
		}
	}

	private class ResourceChangeListener implements IUniformResourceChangeListener
	{

		public void resourceChanged(IUniformResourceChangeEvent event)
		{
			if (resource.equals(event.getResource()))
			{
				update(event.getMarkerDeltas());
			}
		}
	};
}