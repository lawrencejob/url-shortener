"use client"

import { useState } from "react"
import { Button } from "./ui/button"
import { Field, FieldLabel, FieldDescription, FieldError } from "./ui/field"
import { Input } from "./ui/input"
import { useActionState } from "react"
import { shortenAction } from "@/app/actions"
import { Spinner } from "./ui/spinner"
import { IconPlus, IconSend, IconX } from "@tabler/icons-react"
import { CopyNewUrl } from "./copy-new-url"

const initialState = {
    status: null,
    fullUrl: '',
    alias: '',
    errors: {}
}

export default function Form() {
    const [state, formAction, pending] = useActionState(shortenAction, initialState)
    const [hasAlias, setHasAlias] = useState(false)

    if (state?.status === "created") {
        return <CopyNewUrl shortUrl={state.shortUrl} />
    }

    if (state.status == "error") {
        return <>{state.message}</>
    }

    return (
        <form
            action={formAction}
            className="space-y-6"
        >
            <Field>
                <FieldLabel>URL to shorten</FieldLabel>
                <Input
                    placeholder="https://example.com"
                    name="fullUrl"
                    defaultValue={state?.fullUrl || ""}
                    required
                />
                {state.errors?.properties?.fullUrl && <FieldError>Please enter a valid URL, including `https://`</FieldError>}
            </Field>

            {(hasAlias || !!state?.alias) ? (
                <Field>
                    <FieldLabel>Custom alias</FieldLabel>
                    <div className="flex flex-row gap-4 align-bottom">
                        <Input
                            placeholder="e.g. mylink123"
                            name="alias"
                            type="string"
                            minLength={4}
                            maxLength={32}
                            defaultValue={state?.alias || ""}
                        />
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={() => setHasAlias(false)}
                            className="text-sm mt-1 flex-grow-0"
                        ><IconX /> Remove alias </Button>
                    </div>
                    <FieldDescription>
                        4-32 letters or numbers
                    </FieldDescription>
                    {state.errors?.properties?.alias && <FieldError>Aliases must be 4-32 letters or numbers (a-z, A-Z, or 0-9)</FieldError>}
                </Field>
            ) : (
                <Button
                    type="button"
                    variant="secondary"
                    onClick={() => setHasAlias(true)}
                >
                    <IconPlus /> Add custom alias
                </Button>
            )}

            <Button type="submit" className="w-full" disabled={pending}>
                {pending ? <Spinner /> : <><IconSend /> Shorten URL</>}
            </Button>
        </form>
    )
}