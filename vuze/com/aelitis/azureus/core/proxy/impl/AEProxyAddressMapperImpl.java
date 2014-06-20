/*
 * Created on 15-Dec-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.proxy.impl;

import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;

import com.aelitis.azureus.core.proxy.AEProxyAddressMapper;

/**
 * @author parg
 *
 */

public class 
AEProxyAddressMapperImpl
	implements AEProxyAddressMapper
{
	protected static AEProxyAddressMapper	singleton = new AEProxyAddressMapperImpl();
	
	public static AEProxyAddressMapper
	getSingleton()
	{
		return( singleton );
	}
	
	protected boolean	enabled;
	
	protected String	prefix;
	protected long		next_value;
	
	protected Map<String,String>		map			= new HashMap<String,String>();
	protected Map<String,String>		reverse_map	= new HashMap<String,String>();
	
	protected AEMonitor	this_mon	= new AEMonitor( "AEProxyAddressMapper" );
		
	private Map<Integer,PortMappingImpl>	port_mappings = new HashMap<Integer,PortMappingImpl>();
	
	protected
	AEProxyAddressMapperImpl()
	{
	    if ( 	COConfigurationManager.getBooleanParameter("Enable.Proxy") &&
	    		COConfigurationManager.getBooleanParameter("Enable.SOCKS")){
	    	
	    	String	host = COConfigurationManager.getStringParameter("Proxy.Host");
	
	    	try{
		    	if ( 	host.length() > 0 &&
		    			InetAddress.getByName(host).isLoopbackAddress()){
		    		
		    		enabled	= true;
		    		
		    		byte[]	b = new byte[120];
		    		
		    		for (int i=0;i<b.length;i++){
		    			
		    			b[i] = (byte)(RandomUtils.nextInt(256));
		    		}
		    		
		    		prefix = ByteFormatter.encodeString( b );
		    	}
	    	}catch( Throwable e ){
	    		
	    		Debug.printStackTrace(e);
	    	}
	    }
	}
	
	public String
	internalise(
		String	address )
	{
		if ( !enabled ){
			
			return( address );
		}
		
		if ( address.length() < 256 ){
			
			return( address );
		}
		
		String	target;
		
		try{
			this_mon.enter();
			
			target = reverse_map.get( address );
			
			if ( target == null ){
				
				StringBuilder target_b = new StringBuilder( 256 );
				
				target_b.append( prefix );
				target_b.append( next_value++ );
			
				while( target_b.length() < 255 ){
				
					target_b.append( "0" );
				}
				
				target = target_b.toString();
				
				map.put( target, address );
				
				reverse_map.put( address, target );
			}
		}finally{
			
			this_mon.exit();
		}
		
		// System.out.println( "AEProxyAddressMapper: internalise " + address + " -> " + target );
		
		return( target );
	}
	
	public String
	externalise(
		String	address )
	{
		if ( !enabled || address.length() < 255 ){
			
			return( address );
		}
		
		String	target = map.get( address );
		
		if ( target == null ){
			
			target = address;
		}
		
		// System.out.println( "AEProxyAddressMapper: externalise " + address + " -> " + target );
		
		return( target );
	}
	
	public URL
	internalise(
		URL		url )
	{
		if ( !enabled ){
			
			return( url );
		}
		
		String	host = url.getHost();
		
		if ( host.length() < 256 ){
			
			return( url );
		}
		
		String	new_host = internalise( host );
		
		String	url_str = url.toString();
		
		int	pos = url_str.indexOf( host );
		
		if ( pos == -1 ){
			
			Debug.out( "inconsistent url '" + url_str + "' / '" + host + "'" );
			
			return( url );
		}
		
		String 	new_url_str = url_str.substring(0,pos) +
				new_host + url_str.substring(pos+host.length());
		
		try{
			return( new URL( new_url_str ));
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace(e);
			
			return( url );
		}
	}
	
	public URL
	externalise(
		URL		url )
	{
		if ( !enabled ){
			
			return( url );
		}
		
		String	host	= url.getHost();
		
		if ( host.length() < 255 ){
			
			return( url );
		}
		
		String	new_host = externalise( host );
		
		String	url_str = url.toString();
		
		int	pos = url_str.indexOf( host );
		
		if ( pos == -1 ){
			
			Debug.out( "inconsistent url '" + url_str + "' / '" + host + "'" );
			
			return( url );
		}
		
		String 	new_url_str = url_str.substring(0,pos) +
				new_host + url_str.substring(pos+host.length());
		
		try{
			return( new URL( new_url_str ));
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace(e);
			
			return( url );
		}
	}
	
	
	public PortMapping
	registerPortMapping(
		int		local_port,
		String	ip )
	{
		PortMappingImpl mapping = new PortMappingImpl( ip, local_port );
		
		synchronized( port_mappings ){
			
			port_mappings.put( local_port, mapping );
		}
		
		return( mapping );
	}
	
	public InetSocketAddress
	applyPortMapping(
		InetAddress		address,
		int				port )
	{
		InetSocketAddress result;
		
		if ( address.isLoopbackAddress()){
			
			PortMappingImpl mapping;
			
			synchronized( port_mappings ){
				
				mapping = port_mappings.get( port );
			}
			
			if ( mapping == null ){
			
				result = new InetSocketAddress( address, port );
			
			}else{
				
				String ip = mapping.getIP();
				
				if ( AENetworkClassifier.categoriseAddress( ip ) == AENetworkClassifier.AT_PUBLIC ){
					
					result = new InetSocketAddress( ip, port );
					
				}else{
					
						// default to port 6881 here - might need to fix this up one day if this doesn't
						// remain the sensible default
					
					result = InetSocketAddress.createUnresolved( ip, 6881 );
				}
			}
		}else{
			
			result = new InetSocketAddress( address, port );
		}
		
		//System.out.println( "Applying mapping: " + address + "/" + port + " -> " + result );
		
		return( result );
	}
	
	private class
	PortMappingImpl
		implements PortMapping
	{
		private String	ip;
		private int		port;
		
		private
		PortMappingImpl(
			String		_ip,
			int			_port )
		{
			ip		= _ip;
			port	= _port;
		}
		
		private String
		getIP()
		{
			return( ip );
		}
		
		public void 
		unregister() 
		{
			synchronized( port_mappings ){
				
				port_mappings.remove( port );
			}
		}
	}
}
