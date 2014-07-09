/*
 * Created on Aug 26, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.vuze.client.plugins.utp;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;

import com.aelitis.azureus.core.networkmanager.impl.utp.UTPConnectionManager;
import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPPrimordialHandler;

public class 
UTPPlugin
	implements Plugin, PRUDPPrimordialHandler
{
	private PluginInterface		plugin_interface;

	private boolean				logging_enabled;
	private LoggerChannel		log;

	private BooleanParameter	enabled_param;
	
	private UPnPMapping	upnp_mapping;

	private PRUDPPacketHandler handler;

	private UTPConnectionManager manager;
	
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{
		plugin_interface = _plugin_interface;
	
		log = plugin_interface.getLogger().getTimeStampedChannel( "uTP" );

		log.setDiagnostic();
		
		log.setForce(true);

		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "com.vuze.client.plugins.utp.internat.Messages");

		BasicPluginConfigModel	config = plugin_interface.getUIManager().createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, "utp.name" );
			
			// enable
		
		enabled_param = config.addBooleanParameter2( "utp.enabled", "utp.enabled", true );
		
		enabled_param.addListener(
			new org.gudy.azureus2.plugins.ui.config.ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					checkEnabledState();
				}
			});

			// logging
		
		final BooleanParameter logging_param = config.addBooleanParameter2( "utp.logging.enabled", "utp.logging.enabled", false );
		
		logging_param.addListener(
			new org.gudy.azureus2.plugins.ui.config.ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					logging_enabled = logging_param.getValue();
				}
			});
		
		enabled_param.addEnabledOnSelection( logging_param );
		
		logging_enabled = logging_param.getValue();
		
		manager = new UTPConnectionManager( this );

			// prefer uTP
		
		final BooleanParameter prefer_utp_param = config.addBooleanParameter2( "utp.prefer_utp", "utp.prefer_utp", true );
		
		prefer_utp_param.addListener(
			new org.gudy.azureus2.plugins.ui.config.ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					manager.preferUTP(prefer_utp_param.getValue());
				}
			});
		
		manager.preferUTP(prefer_utp_param.getValue());
		
		enabled_param.addEnabledOnSelection( prefer_utp_param );

		if ( manager.getProviderVersion() > 1 ){
			
			final IntParameter recv_buff_size = config.addIntParameter2( "utp.recv.buff", "utp.recv.buff", UTPConnectionManager.DEFAULT_RECV_BUFFER_KB, 0, 5*1024 );
			final IntParameter send_buff_size = config.addIntParameter2( "utp.send.buff", "utp.send.buff", UTPConnectionManager.DEFAULT_SEND_BUFFER_KB, 0, 5*1024 );
			
			recv_buff_size.addListener(
					new org.gudy.azureus2.plugins.ui.config.ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param )
						{
							manager.setReceiveBufferSize( recv_buff_size.getValue());
						}
					});
					
			send_buff_size.addListener(
					new org.gudy.azureus2.plugins.ui.config.ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param )
						{
							manager.setSendBufferSize( recv_buff_size.getValue());
						}
					});
			
			manager.setReceiveBufferSize( recv_buff_size.getValue());
			manager.setSendBufferSize( send_buff_size.getValue());
		
			enabled_param.addEnabledOnSelection( recv_buff_size );
			enabled_param.addEnabledOnSelection( send_buff_size );
		}
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						COConfigurationManager.addAndFireParameterListeners(
								new String[]{ "TCP.Listen.Port", "TCP.Listen.Port.Enable" },
								new ParameterListener()
								{
									public void
									parameterChanged(
										String	name )
									{
										checkEnabledState();
									}
								});
					}
										
					public void
					closedownInitiated()
					{
					}
					
					public void
					closedownComplete()
					{
					}
				});
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	private void
	checkEnabledState()
	{
		boolean	plugin_enabled	= enabled_param.getValue();
		
		boolean	tcp_enabled 	= COConfigurationManager.getBooleanParameter( "TCP.Listen.Port.Enable" );
		
		if ( tcp_enabled && plugin_enabled ){
			
			log( "Plugin is enabled: version=" + plugin_interface.getPluginVersion());
			
			if ( upnp_mapping != null ){
				
				upnp_mapping.destroy();
			}
			
			int	port = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );				

			PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
			
			if ( pi_upnp == null ){

				log( "UPnP plugin not found, can't map port" );
				
			}else{
				
				upnp_mapping = 
					((UPnPPlugin)pi_upnp.getPlugin()).addMapping( 
						plugin_interface.getPluginName(), 
						false, 
						port, 
						true );
				
				log( "UPnP mapping registered for port " + port );
			}
			
			if ( handler == null || port != handler.getPort()){
				
				if ( handler != null ){
					
					handler.removePrimordialHandler( UTPPlugin.this );
				}
					
				handler = PRUDPPacketHandlerFactory.getHandler( port );
				
				handler.addPrimordialHandler( UTPPlugin.this );				
			}
			
			manager.activate( handler );
			
		}else{
			
			log( "Plugin is disabled" );
			
			if ( handler != null ){
				
				handler.removePrimordialHandler( UTPPlugin.this  );
				
				handler = null;
			}
			
			if ( upnp_mapping != null ){
				
				upnp_mapping.destroy();
				
				upnp_mapping = null;
			}
			
			manager.deactivate();
		}
	}
	
	public boolean
	packetReceived(
		DatagramPacket	packet )
	{
		return( manager.receive((InetSocketAddress)packet.getSocketAddress(), packet.getData(), packet.getLength()));
	}
	
	public boolean
	send(
		InetSocketAddress	to,
		byte[]				buffer,
		int					length )
	{
		if ( length != buffer.length ){
			
			Debug.out( "optimise this" );
			
			byte[] temp = new byte[length];
			
			System.arraycopy(buffer, 0, temp, 0, length );
			
			buffer = temp;
		}
		
		try{
			handler.primordialSend( buffer, to );
			
		}catch( Throwable e ){
			
			return( false );
		}

		return( true );
	}
	
	public void
	log(
		String		str )
	{
		if ( logging_enabled ){
		
			log.log( str );
		}
	}
	
	public void
	log(
		String		str,
		Throwable 	e )
	{		
		log.log( str, e );
		
		Debug.out( e );
	}
}
