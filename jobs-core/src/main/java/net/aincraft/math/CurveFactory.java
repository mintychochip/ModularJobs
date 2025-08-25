package net.aincraft.math;

import net.aincraft.Job.LevelingCurve;
import net.aincraft.Job.PayableCurve;

public interface CurveFactory {

  LevelingCurve create(LevelingCurve.Parameters parameters);

  PayableCurve create(PayableCurve.Parameters parameters);
}
