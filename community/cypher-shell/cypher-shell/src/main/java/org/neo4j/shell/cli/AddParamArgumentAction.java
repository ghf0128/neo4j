/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.shell.cli;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.shell.parameter.ParameterService;
import org.neo4j.shell.parameter.ParameterService.ParameterParser;
import org.neo4j.shell.parameter.ParameterService.RawParameter;

/**
 * Action that parses and appends query parameters.
 */
public class AddParamArgumentAction implements ArgumentAction
{
    private final ParameterParser queryParameterParser;

    AddParamArgumentAction( ParameterParser queryParameterParser )
    {
        this.queryParameterParser = queryParameterParser;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void run( ArgumentParser parser, Argument arg, Map<String,Object> attrs, String flag, Object value )
            throws ArgumentParserException
    {
        Object param = attrs.get( arg.getDest() );
        if ( param instanceof List )
        {
            ((List<RawParameter>) param).add( parse( value.toString() ) );
        }
        else
        {
            var queryParams = new ArrayList<RawParameter>();
            queryParams.add( parse( value.toString() ) );
            attrs.put( arg.getDest(), queryParams );
        }
    }

    private RawParameter parse( String input )
    {
        try
        {
            return queryParameterParser.parse( input );
        }
        catch ( ParameterService.ParameterParsingException e )
        {
            throw new IllegalArgumentException( "Incorrect usage.\nusage: --param  'name => value'" );
        }
    }

    @Override
    public void onAttach( Argument arg )
    {
    }

    @Override
    public boolean consumeArgument()
    {
        return true;
    }
}
