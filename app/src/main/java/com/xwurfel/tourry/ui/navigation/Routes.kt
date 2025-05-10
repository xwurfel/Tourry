package com.xwurfel.tourry.ui.navigation

object Routes {
    const val AUTH = "auth"
    const val LOGIN = "$AUTH/login"
    const val REGISTER = "$AUTH/register"

    const val HOME = "home"

    const val TOUR_DISCOVERY = "tour_discovery"
    const val TOUR_DETAILS = "tour_details/{tourId}"

    const val TOUR_BUILDER = "tour_builder"
    const val TOUR_BUILDER_CREATE = "$TOUR_BUILDER/create"
    const val TOUR_BUILDER_EDIT = "$TOUR_BUILDER/edit/{tourId}"
    const val TOUR_BUILDER_SAVED = "$TOUR_BUILDER/saved"//{tourId}"

    const val TOUR_TRACKING = "tour_tracking/{groupId}"

    const val PROFILE = "profile"
}