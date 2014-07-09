/*
 * Created on Jan 23, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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

import org.gudy.azureus2.core3.util.Constants;

import com.vuze.client.plugins.utp.core.UTPProviderNative;
import com.vuze.client.plugins.utp.loc.UTPProviderLocal;
//import com.vuze.client.plugins.utp.test.UTPProviderTester;


public class 
UTPProviderFactory 
{
	// remove this once 4813 is released
	
	public static final boolean	isAndroid;
	  
	static{
	
		String vm_name = System.getProperty( "java.vm.name", "" );
		  
		isAndroid = vm_name.equalsIgnoreCase( "Dalvik" );
	}
	  
	public static UTPProvider
	createProvider()
	{
		if ( isAndroid || Constants.IS_CVS_VERSION || Constants.isCurrentVersionGE( "5.0.0.0" )){
			
			return( new UTPProviderLocal());
			
		}else if ( Constants.isOSX || Constants.isWindows ){
		
			return( new UTPProviderNative());
			
		}else{
			
			return( new UTPProviderLocal());
		}
		
		//return( new UTPProviderTester( new UTPProviderLocal( true )));
		//return( new UTPProviderTester( new UTPProviderNative( true )));
	}
}
