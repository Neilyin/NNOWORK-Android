package com.neilyin.nnowork.feature.treehole

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.neilyin.nnowork.data.treehole.TreeHoleIndustryCatalog
import com.neilyin.nnowork.data.treehole.TreeHoleIndustryIconKey

/** 24 個 canonical 職業對應的 Material 圖示；未知舊值維持通用工作圖示。 */
internal fun treeHoleIndustryIcon(canonicalValue: String?): ImageVector =
    when (TreeHoleIndustryCatalog.find(canonicalValue)?.iconKey) {
        TreeHoleIndustryIconKey.TECHNOLOGY -> Icons.Filled.Computer
        TreeHoleIndustryIconKey.FINANCE -> Icons.Filled.AccountBalance
        TreeHoleIndustryIconKey.FOOD_BEVERAGE -> Icons.Filled.Restaurant
        TreeHoleIndustryIconKey.RETAIL -> Icons.Filled.Storefront
        TreeHoleIndustryIconKey.EDUCATION -> Icons.Filled.School
        TreeHoleIndustryIconKey.HEALTHCARE -> Icons.Filled.MedicalServices
        TreeHoleIndustryIconKey.MANUFACTURING -> Icons.Filled.PrecisionManufacturing
        TreeHoleIndustryIconKey.MEDIA -> Icons.Filled.Movie
        TreeHoleIndustryIconKey.SERVICE -> Icons.Filled.RoomService
        TreeHoleIndustryIconKey.CIVIL_SERVICE -> Icons.Filled.Badge
        TreeHoleIndustryIconKey.CONSTRUCTION -> Icons.Filled.Construction
        TreeHoleIndustryIconKey.LOGISTICS -> Icons.Filled.LocalShipping
        TreeHoleIndustryIconKey.HOSPITALITY -> Icons.Filled.Luggage
        TreeHoleIndustryIconKey.BEAUTY -> Icons.Filled.ContentCut
        TreeHoleIndustryIconKey.DESIGN -> Icons.Filled.Palette
        TreeHoleIndustryIconKey.LEGAL -> Icons.Filled.Gavel
        TreeHoleIndustryIconKey.REAL_ESTATE -> Icons.Filled.Apartment
        TreeHoleIndustryIconKey.MARKETING_PR -> Icons.Filled.Campaign
        TreeHoleIndustryIconKey.INSURANCE -> Icons.Filled.HealthAndSafety
        TreeHoleIndustryIconKey.AGRICULTURE -> Icons.Filled.Agriculture
        TreeHoleIndustryIconKey.AVIATION -> Icons.Filled.Flight
        TreeHoleIndustryIconKey.SOCIAL_WORK -> Icons.Filled.VolunteerActivism
        TreeHoleIndustryIconKey.FREELANCE -> Icons.Filled.LaptopMac
        TreeHoleIndustryIconKey.OTHER -> Icons.Filled.MoreHoriz
        null -> Icons.Filled.Work
    }
