/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ynov.helloworld.ui.icons

// Icône Material copiée depuis material-icons-extended 1.7.8 (Apache 2.0) : seule cette
// icône est embarquée, au lieu de la bibliothèque complète (plusieurs milliers d'icônes).

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.LocationOff: ImageVector
    get() {
        if (_locationOff != null) {
            return _locationOff!!
        }
        _locationOff = materialIcon(name = "Outlined.LocationOff") {
            materialPath {
                moveTo(12.0f, 4.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                curveToRelative(0.0f, 1.06f, -0.39f, 2.32f, -1.0f, 3.62f)
                lineToRelative(1.49f, 1.49f)
                curveTo(18.37f, 12.36f, 19.0f, 10.57f, 19.0f, 9.0f)
                curveToRelative(0.0f, -3.87f, -3.13f, -7.0f, -7.0f, -7.0f)
                curveToRelative(-1.84f, 0.0f, -3.5f, 0.71f, -4.75f, 1.86f)
                lineToRelative(1.43f, 1.43f)
                curveTo(9.56f, 4.5f, 10.72f, 4.0f, 12.0f, 4.0f)
                close()
                moveTo(12.0f, 6.5f)
                curveToRelative(-0.59f, 0.0f, -1.13f, 0.21f, -1.56f, 0.56f)
                lineToRelative(3.5f, 3.5f)
                curveToRelative(0.35f, -0.43f, 0.56f, -0.97f, 0.56f, -1.56f)
                curveToRelative(0.0f, -1.38f, -1.12f, -2.5f, -2.5f, -2.5f)
                close()
                moveTo(3.41f, 2.86f)
                lineTo(2.0f, 4.27f)
                lineToRelative(3.18f, 3.18f)
                curveTo(5.07f, 7.95f, 5.0f, 8.47f, 5.0f, 9.0f)
                curveToRelative(0.0f, 5.25f, 7.0f, 13.0f, 7.0f, 13.0f)
                reflectiveCurveToRelative(1.67f, -1.85f, 3.38f, -4.35f)
                lineTo(18.73f, 21.0f)
                lineToRelative(1.41f, -1.41f)
                lineTo(3.41f, 2.86f)
                close()
                moveTo(12.0f, 18.88f)
                curveToRelative(-2.01f, -2.58f, -4.8f, -6.74f, -4.98f, -9.59f)
                lineToRelative(6.92f, 6.92f)
                curveToRelative(-0.65f, 0.98f, -1.33f, 1.89f, -1.94f, 2.67f)
                close()
            }
        }
        return _locationOff!!
    }

private var _locationOff: ImageVector? = null
