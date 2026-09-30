package com.saney.renaultdocs

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

class CreateProjectActivity : Activity() {
    private lateinit var store: ProjectStore
    private lateinit var nameInput: EditText
    private lateinit var statusText: TextView

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

        savedInstanceState
            ?.getString(
                STATE_PROJECT_NAME,
            )
            ?.let {
                nameInput.setText(
                    it,
                )
                nameInput.setSelection(
                    nameInput.text.length,
                )
            }
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
                    "Новий проєкт Renault",
                sizeSp =
                    24f,
            ).apply {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD,
                )
                setPadding(
                    Ui.dp(
                        this@CreateProjectActivity,
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
                    "Проєкт — це модель автомобіля. Наприклад: Scenic II.",
                sizeSp =
                    15f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@CreateProjectActivity,
                        10,
                    ),
                    0,
                    Ui.dp(
                        this@CreateProjectActivity,
                        12,
                    ),
                )
            },
        )

        nameInput =
            EditText(
                this,
            ).apply {
                hint =
                    "Назва проєкту"
                setSingleLine(
                    true,
                )
            }

        root.addView(
            nameInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        root.addView(
            Button(
                this,
            ).apply {
                text =
                    "Створити проєкт"
                isAllCaps =
                    false
                setOnClickListener {
                    createProject()
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    Ui.dp(
                        this@CreateProjectActivity,
                        10,
                    )
            },
        )

        statusText =
            Ui.textView(
                context =
                    this,
                value =
                    "",
                sizeSp =
                    14f,
                color =
                    Ui.muted,
            ).apply {
                setPadding(
                    0,
                    Ui.dp(
                        this@CreateProjectActivity,
                        12,
                    ),
                    0,
                    0,
                )
            }

        root.addView(
            statusText,
        )

        return root
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putString(
            STATE_PROJECT_NAME,
            nameInput.text
                .toString(),
        )
        super.onSaveInstanceState(
            outState,
        )
    }

    private fun createProject() {
        runCatching {
            store.createProject(
                nameInput.text
                    .toString(),
            )
        }
            .onSuccess {
                project ->
                startActivity(
                    ProjectActivity.intent(
                        context =
                            this,
                        projectId =
                            project.id,
                    ),
                )
                finish()
            }
            .onFailure {
                error ->
                statusText.text =
                    error.message
                        ?: "Не вдалося створити проєкт."
            }
    }
}
