package com.trainiq.domain.repository

/** A referenced library entry no longer exists; retry requires editing the meal draft. */
class UnavailableMealItemException : IllegalStateException("Deze maaltijd bevat een verwijderd product of recept.")

class InvalidMealItemException : IllegalStateException("Deze maaltijd bevat een onvolledig item.")
