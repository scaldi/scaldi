package org.am.scaldi.core.example.module

import java.awt.Color
import org.am.scaldi.core.example.service.{AppleFarm, FastidiousGranny, Granny}
import org.am.scaldi.core.example.model.Apple
import org.am.scaldi.core.Provider
/**
 * 
 * @author Oleg Ilyenko
 */
trait FarmModule {
    lazy val normalGranny = new Granny(Provider(Apple(Color.ORANGE)))

    lazy val fastidiousGranny = new FastidiousGranny(Color.RED, Provider((desiredColor: Color) => desiredColor match{
        case Color.RED | Color.GREEN => Apple(desiredColor)
        case color => throw new IllegalStateException("Sorry, no apples of color: " + color)
    }))

    lazy val farm = new AppleFarm(List(normalGranny, fastidiousGranny))
}