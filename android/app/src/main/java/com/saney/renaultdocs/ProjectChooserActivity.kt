package com.saney.renaultdocs

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView

class ProjectChooserActivity : Activity() {
    private lateinit var store: ProjectStore
    private lateinit var container: LinearLayout

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        store =
            ProjectStore(
                this,
            )

        setContentView(
            buildContent(),
        )
        render()
    }

    private fun buildContent():
        View {
        window.statusBarColor =
            Ui.background
        window.navigationBarColor =
            Ui.background

        val root =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(
                    Ui.background,
                )
            }

        Ui.applySystemInsets(
            root,
        )

        val topBar =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        topBar.addView(
            ImageButton(
                this,
            ).apply {
                setImageResource(
                    R.drawable.ic_arrow_back,
                )
                contentDescription =
                    "Назад"
                setBackgroundColor(
                    android.graphics.Color.TRANSPARENT,
                )
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                Ui.dp(
                    this,
                    48,
                ),
                Ui.dp(
                    this,
                    48,
                ),
            ),
        )

        topBar.addView(
            Ui.textView(
                context =
                    this,
                value =
                    "Вибрати проєкт",
                sizeSp =
                    24f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                setPadding(
                    Ui.dp(
                        this@ProjectChooserActivity,
                        8,
                    ),
                    0,
                    0,
                    0,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        root.addView(
            topBar,
        )

        root.addView(
            Ui.textView(
                context =
                    this,
                value =
                    "До якого проєкту додати том?",
                sizeSp =
                    15f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@ProjectChooserActivity,
                        8,
                    ),
                    0,
                    Ui.dp(
                        this@ProjectChooserActivity,
                        12,
                    ),
                )
            },
        )

        val scroll =
            ScrollView(
                this,
            )

        container =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        scroll.addView(
            container,
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        return root
    }

    private fun render() {
        container.removeAllViews()

        store.projects()
            .forEach {
                project ->
                container.addView(
                    LinearLayout(
                        this,
                    ).apply {
                        orientation =
                            LinearLayout.VERTICAL
                        isClickable =
                            true
                        isFocusable =
                            true
                        background =
                            Ui.roundedBackground(
                                context =
                                    this@ProjectChooserActivity,
                                fill =
                                    Ui.surface,
                                stroke =
                                    Ui.border,
                            )
                        setPadding(
                            Ui.dp(
                                this@ProjectChooserActivity,
                                18,
                            ),
                            Ui.dp(
                                this@ProjectChooserActivity,
                                16,
                            ),
                            Ui.dp(
                                this@ProjectChooserActivity,
                                18,
                            ),
                            Ui.dp(
                                this@ProjectChooserActivity,
                                16,
                            ),
                        )

                        addView(
                            Ui.textView(
                                context =
                                    this@ProjectChooserActivity,
                                value =
                                    project.title,
                                sizeSp =
                                    20f,
                                color =
                                    Ui.entityTitle,
                            ).apply {
                                setTypeface(
                                    typeface,
                                    android.graphics.Typeface.BOLD,
                                )
                            },
                        )

                        addView(
                            Ui.textView(
                                context =
                                    this@ProjectChooserActivity,
                                value =
                                    "Томів: " +
                                        store.volumes(
                                            project.id,
                                        ).size,
                                sizeSp =
                                    14f,
                                color =
                                    Ui.accent,
                            ).apply {
                                setPadding(
                                    0,
                                    Ui.dp(
                                        this@ProjectChooserActivity,
                                        6,
                                    ),
                                    0,
                                    0,
                                )
                            },
                        )

                        setOnClickListener {
                            startActivity(
                                ProjectActivity.intent(
                                    context =
                                        this@ProjectChooserActivity,
                                    projectId =
                                        project.id,
                                    openPicker =
                                        true,
                                ),
                            )
                            finish()
                        }
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin =
                            Ui.dp(
                                this@ProjectChooserActivity,
                                10,
                            )
                    },
                )
            }
    }
}
