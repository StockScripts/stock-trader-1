package com.effinggames.modules.backtest

class Portfolio(_cash: Double, positionMap: Map[Stock, Position] = Map()) {
  def floatingCash: Double = _cash

  //Gets the total market value of a seq of positions.
  private def calcPositionsValue(positions: Seq[Position]): Double = positions.foldLeft(0d){
    case (total, position) =>
      total + position.quantity * position.stock.getPrice
  }

  def positionsValue: Double = calcPositionsValue(positionMap.values.toSeq)

  def totalValue: Double = floatingCash + positionsValue

  def positions: Map[Stock, Position] = positionMap

  /**
    * Creates a new portfolio object with the added position.
    * To remove positions, use this method with negative position quantities.
    * @param position The position to add to the portfolio.
    * @return Returns an immutable portfolio object with the added positions.
    */
  private[backtest] def addPosition(position: Position): Portfolio = {
    position.quantity match {
      case 0 => this
      case _ =>
        val totalCost = calcPositionsValue(Seq(position))
        val newCashValue = floatingCash - totalCost

        //Gets current position, if applicable, and adds it to new ones.
        val updatedPosition = positionMap.get(position.stock) match {
            case Some(oldPosition) =>
              val newAmount = oldPosition.quantity + position.quantity
              //Updates cost basis, if positions are being sold then cost basis is unaffected.
              val newCostBasis = if (position.quantity >= 0) {
                (oldPosition.costBasis * oldPosition.quantity + position.costBasis * position.quantity) / newAmount
              } else {
                oldPosition.costBasis
              }
              Position(position.stock, newAmount, newCostBasis)
            case None =>
              position
          }
        val newPositionMap = positionMap + (updatedPosition.stock -> updatedPosition)
        new Portfolio(newCashValue, newPositionMap)
    }
  }
}

case class Position (
  stock: Stock,
  quantity: Int,
  costBasis: Double
)