/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.wallet;

/**
 * Type of wallet fund - cash, bonus (cash), or Freeround
 */
public enum FundType {
	CASH,
	BONUS,
	FREEROUNDS,
	OPERATOR_FREEROUNDS,
	/*Added to support possible expansion of Mesh fund types not implemented in Hive
	 * Allows Wallet Funds to add up to total balance.
	 * */
	UNKNOWN
}
