package org.apache.cassandra.cql.jdbc;
/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */


import org.apache.cassandra.cql.ConnectionDetails;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import org.apache.cassandra.cql.Schema;

import static org.apache.cassandra.utils.Hex.bytesToHex;


public class PreparedStatementTest
{ 
    private static java.sql.Connection con = null;
    private static final Schema schema = new Schema(ConnectionDetails.getHost(), ConnectionDetails.getPort());
    
    @BeforeClass
    public static void waxOn() throws Exception
    {
        schema.createSchema();
        Class.forName("org.apache.cassandra.cql.jdbc.CassandraDriver");
        con = DriverManager.getConnection(String.format("jdbc:cassandra://%s:%d/%s", ConnectionDetails.getHost(), ConnectionDetails.getPort(), Schema.KEYSPACE_NAME));
    }
    
    @Test
    public void testBytes() throws SQLException
    {
        // insert
        PreparedStatement stmt = con.prepareStatement("update JdbcBytes set ?=?, ?=? where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setBytes(1, toByteArray(i));
            stmt.setBytes(2, toByteArray((i+1)*10));
            stmt.setBytes(3, toByteArray(i+100));
            stmt.setBytes(4, toByteArray((i+1)*10+1));
            stmt.setBytes(5, key);
            stmt.executeUpdate();
        }
        
        // verify
        stmt = con.prepareStatement("select ?, ? from JdbcBytes where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setBytes(1, toByteArray(i));
            stmt.setBytes(2, toByteArray(i+100));
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            assert rs.next();
            assert Arrays.equals(rs.getBytes(bytesToHex(toByteArray(i))), toByteArray((i+1)*10));
            assert Arrays.equals(rs.getBytes(bytesToHex(toByteArray(i+100))), toByteArray((i+1)*10+1));
            assert Arrays.equals(rs.getBytes(1), toByteArray((i+1)*10));
            assert Arrays.equals(rs.getBytes(2), toByteArray((i+1)*10+1));
            assert !rs.next();
            rs.close();
        }
        
        // delete
        stmt = con.prepareStatement("delete ?, ? from JdbcBytes where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setBytes(1, toByteArray(i));
            stmt.setBytes(2, toByteArray(i+100));
            stmt.setBytes(3, key);
            stmt.execute();
        }
        
        // verify
        stmt = con.prepareStatement("select ?, ? from JdbcBytes where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setBytes(1, toByteArray(i));
            stmt.setBytes(2, toByteArray(i+100));
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            assert rs.getString(1) == null;  assert rs.getString(2) == null;
            rs.close();
        }
    }
    
    @Test
    public void testUtf8() throws SQLException
    {
        // insert.
        PreparedStatement stmt = con.prepareStatement("update JdbcUtf8 set ?=?, ?=? where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1\u6543\u3435\u6554");
            stmt.setString(2, "abc\u6543\u3435\u6554");
            stmt.setString(3, "2\u6543\u3435\u6554");
            stmt.setString(4, "def\u6543\u3435\u6554");
            stmt.setBytes(5, key);
            stmt.executeUpdate();
        }
        
        // verify
        stmt = con.prepareStatement("select ?, ? from JdbcUtf8 where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1\u6543\u3435\u6554");
            stmt.setString(2, "2\u6543\u3435\u6554");
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            assert rs.next();
            assert rs.getString("1\u6543\u3435\u6554").equals("abc\u6543\u3435\u6554");
            assert rs.getString("2\u6543\u3435\u6554").equals("def\u6543\u3435\u6554");
            assert rs.getString(1).equals("abc\u6543\u3435\u6554");
            assert rs.getString(2).equals("def\u6543\u3435\u6554");
            assert !rs.next();
            rs.close();
        }
        
        // delete
        stmt = con.prepareStatement("delete ?, ? from JdbcUtf8 where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1\u6543\u3435\u6554");
            stmt.setString(2, "2\u6543\u3435\u6554");
            stmt.setBytes(3, key);
            stmt.execute();
        }
        
        // verify
        stmt = con.prepareStatement("select ?, ? from JdbcUtf8 where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1\u6543\u3435\u6554");
            stmt.setString(2, "2\u6543\u3435\u6554");
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            assert rs.getString(1) == null;  assert rs.getString(2) == null;
            rs.close();
        }
    }
    
    @Test
    public void testAscii() throws SQLException
    {
        // insert.
        PreparedStatement stmt = con.prepareStatement("update JdbcAscii set ?=?, ?=? where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1");
            stmt.setString(2, "abc");
            stmt.setString(3, "2");
            stmt.setString(4, "def");
            stmt.setBytes(5, key);
            stmt.executeUpdate();
        }
        
        // verify
        stmt = con.prepareStatement("select ?, ? from JdbcAscii where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1");
            stmt.setString(2, "2");
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            assert rs.next();
            assert rs.getString("1").equals("abc");
            assert rs.getString("2").equals("def");
            assert rs.getString(1).equals("abc");
            assert rs.getString(2).equals("def");
            assert !rs.next();
            rs.close();
        }
        
        // delete
        stmt = con.prepareStatement("delete ?, ? from JdbcAscii where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1");
            stmt.setString(2, "2");
            stmt.setBytes(3, key);
            stmt.execute();
        }
        
        // verify
        stmt = con.prepareStatement("select ?, ? from JdbcAscii where key=?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setString(1, "1");
            stmt.setString(2, "2");
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            assert rs.getString(1) == null;  assert rs.getString(2) == null;
            rs.close();
        }
    }
    
    @Test
    public void testLong() throws SQLException
    {
        PreparedStatement stmt = con.prepareStatement("update JdbcLong set ?=?, ?=? where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setLong(1, 1);
            stmt.setLong(2, (i+1)*10);
            stmt.setLong(3, 2);
            stmt.setLong(4, (i+1)*10+1);
            stmt.setBytes(5, key);
            stmt.executeUpdate();
        }
        stmt.close();
        
        // verify.
        stmt = con.prepareStatement("select ?, ? from JdbcLong where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setLong(1, 1);
            stmt.setLong(2, 2);
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            assert rs.next();
            assert rs.getLong("1") == (i+1)*10;
            assert rs.getLong("2") == (i+1)*10+1;
            assert rs.getLong(1) == (i+1)*10;
            assert rs.getLong(2) == (i+1)*10+1;
            assert !rs.next();
            rs.close();
        }
        
        // delete
        stmt = con.prepareStatement("delete ?, ? from JdbcLong where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setLong(1, 1);
            stmt.setLong(2, 2);
            stmt.setBytes(3, key);
            stmt.execute();
        }
        
        // verify.
        stmt = con.prepareStatement("select ?, ? from JdbcLong where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setLong(1, 1);
            stmt.setLong(2, 2);
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            rs.getLong(1);
            assert rs.wasNull();
            rs.close();
        }
    }
    
    @Test
    public void testInteger() throws SQLException
    {
        PreparedStatement stmt = con.prepareStatement("update JdbcInteger set ?=?, ?=? where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setInt(1, 1);
            stmt.setInt(2, (i+1)*10);
            stmt.setInt(3, 2);
            stmt.setInt(4, (i+1)*10+1);
            stmt.setBytes(5, key);
            stmt.executeUpdate();
        }
        stmt.close();
        
        // verify.
        stmt = con.prepareStatement("select ?, ? from JdbcInteger where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setInt(1, 1);
            stmt.setInt(2, 2);
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            assert rs.next();
            assert rs.getInt("1") == (i+1)*10;
            assert rs.getInt("2") == (i+1)*10+1;
            assert rs.getInt(1) == (i+1)*10;
            assert rs.getInt(2) == (i+1)*10+1;
            assert !rs.next();
            rs.close();
        }
        
        // delete
        stmt = con.prepareStatement("delete ?, ? from JdbcInteger where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setInt(1, 1);
            stmt.setInt(2, 2);
            stmt.setBytes(3, key);
            stmt.execute();
        }
        
        // verify.
        stmt = con.prepareStatement("select ?, ? from JdbcInteger where key = ?");
        for (int i = 0; i < 5; i++)
        {
            byte[] key = Integer.toString(i).getBytes();
            stmt.setInt(1, 1);
            stmt.setInt(2, 2);
            stmt.setBytes(3, key);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            rs.getInt(1);
            assert rs.wasNull();
            rs.close();
        }
    }
    
    @Test
    public void testParamSubstitution() throws SQLException
    {
        byte[] key = "key".getBytes();
        String q = "SELECT 'fo??est', ?, ? from JdbcUtf8 WHERE KEY = ?";
        CassandraPreparedStatement stmt = (CassandraPreparedStatement)con.prepareStatement(q);
        stmt.setString(1, "pathological param: ?'make it?? '' sto'p?'");
        stmt.setString(2, "simple");
        stmt.setBytes(3, key);
        String qq = stmt.makeCql();
        assert qq.equals("SELECT 'fo??est', 'pathological param: ?''make it?? '''' sto''p?''', 'simple' from JdbcUtf8 WHERE KEY = '6b6579'");
        
        q = "UPDATE JdbcUtf8 USING CONSISTENCY ONE SET 'fru??us'=?, ?='gr''d?', ?=?, ?=? WHERE key=?";
        stmt = (CassandraPreparedStatement)con.prepareStatement(q);
        stmt.setString(1, "o?e");
        stmt.setString(2, "tw'o");
        stmt.setString(3, "thr'?'ee");
        stmt.setString(4, "fo''?'ur");
        stmt.setString(5, "five");
        stmt.setString(6, "six");
        stmt.setBytes(7, key);
        qq = stmt.makeCql();
        assert qq.equals("UPDATE JdbcUtf8 USING CONSISTENCY ONE SET 'fru??us'='o?e', 'tw''o'='gr''d?', 'thr''?''ee'='fo''''?''ur', 'five'='six' WHERE key='6b6579'");
        
        q = "DELETE ?, ? FROM JdbcUtf8 WHERE KEY=?";
        stmt = (CassandraPreparedStatement)con.prepareStatement(q);
        stmt.setString(1, "on'?'");
        stmt.setString(2, "two");
        stmt.setBytes(3, key);
        qq = stmt.makeCql();
        assert qq.equals("DELETE 'on''?''', 'two' FROM JdbcUtf8 WHERE KEY='6b6579'");
    }

    /**
     * Copy bytes from int into bytes starting from offset.
     * @param bytes Target array
     * @param offset Offset into the array
     * @param i Value to write
     */
    public static void copyIntoBytes(byte[] bytes, int offset, int i)
    {
        bytes[offset]   = (byte)( ( i >>> 24 ) & 0xFF );
        bytes[offset+1] = (byte)( ( i >>> 16 ) & 0xFF );
        bytes[offset+2] = (byte)( ( i >>> 8  ) & 0xFF );
        bytes[offset+3] = (byte)(   i          & 0xFF );
    }

    /**
     * @param i Write this int to an array
     * @return 4-byte array containing the int
     */
    public static byte[] toByteArray(int i)
    {
        byte[] bytes = new byte[4];
        copyIntoBytes(bytes, 0, i);
        return bytes;
    }
}
