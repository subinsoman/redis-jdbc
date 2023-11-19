/**
 * 
 */
package org.apache.redis.jdbc;

import java.sql.SQLException;

/**
 * 
 */
public class RedisDriver extends NonRegisteringDriver implements java.sql.Driver{

	// ~ Static fields/initializers
		// ---------------------------------------------

		//
		// Register ourselves with the DriverManager
		//
		static {
			try {
				java.sql.DriverManager.registerDriver(new RedisDriver());
			} catch (SQLException E) {
				throw new RuntimeException("Can't register driver!");
			}
		}

		// ~ Constructors
		// -----------------------------------------------------------

		/**
		 * Construct a new driver and register it with DriverManager
		 * 
		 * @throws SQLException
		 *             if a database error occurs.
		 */
		public RedisDriver() throws SQLException {
			// Required for Class.forName().newInstance()
		}

}
